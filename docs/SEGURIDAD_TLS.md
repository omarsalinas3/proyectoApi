# Seguridad TLS de la integración con GestoPago

Este documento explica cómo se protege el canal de comunicación en los dos sentidos y por qué se tomó cada decisión.

| Sentido | Qué protege | Dónde está |
|---|---|---|
| **Cliente → nuestra API** | Credenciales de login, JWT, código 2FA, datos de la transacción | Render (certificado público) y perfil `https` en local (certificado propio) |
| **Nuestra API → GestoPago** | Token Bearer, contraseña del distribuidor, payload cifrado de sendTx/confirmTx | `GestoPagoTlsConfig` + `tls/gestopago-ca.pem` |

---

## 1. Peticiones hacia GestoPago

### Qué se hizo

- **Truststore propio** (`src/main/resources/tls/gestopago-ca.pem`). La API solo confía en las CA raíz de DigiCert que emiten el certificado de `gestopago.portalventas.net`, no en las ~140 CA que trae la JVM. Si alguien intercepta la conexión con un certificado de otra CA, aunque sea válido para el navegador, la conexión se rechaza.
- **Solo TLS 1.3 y 1.2** (`ProtocolosTlsSocketFactory`). Si el servidor solo ofreciera TLS 1.1 o anterior, la conexión falla en lugar de degradarse.
- **Verificación del nombre de host activa.** Se usa la verificación estándar de Java; nunca un `HostnameVerifier` permisivo ni un `TrustManager` que acepte todo.
- **Un solo cliente HTTP para todos los `@FeignClient`** (token, catálogo, sendTx y confirmTx): ninguna petición a GestoPago sale por otro canal.
- **Verificación al arrancar** (`GestoPagoTlsVerificador`). Hace solo el handshake, sin enviar ninguna petición HTTP, así que no consume los límites diarios de GestoPago. Registra el protocolo, el emisor y los días que le quedan al certificado, y avisa si vence pronto.

### Por qué se fija la CA raíz y no el certificado del servidor

El certificado de GestoPago (`CN=*.portalventas.net`) dura un año. El que estaba vigente al hacer este cambio vencía el **1 de noviembre de 2026**. Si se fijara ese certificado, la integración dejaría de funcionar el día que GestoPago lo renueve. Fijando la raíz de DigiCert (G2, más Global Root CA y G3 como respaldo dentro del mismo emisor), la renovación se acepta sola y se sigue rechazando cualquier certificado de otra CA.

Si GestoPago cambiara de CA, se reemplaza el PEM con la variable `GESTOPAGO_TLS_CA=file:/ruta/nuevo.pem`, sin tocar el código.

### Origen de los certificados

Las tres raíces se exportaron del almacén de confianza del JDK (`keytool -exportcert -cacerts`), no se descargaron de la red. Se comprobó que la huella SHA-256 de DigiCert Global Root G2 coincide con la que envía el servidor de GestoPago:

```
CB:3C:CB:B7:60:31:E5:E0:13:8F:8D:D3:9A:23:F9:DE:47:FF:C3:5E:43:C1:14:4C:EA:27:D4:6A:5A:B1:CB:5F
```

### Qué pasa si falla el TLS durante un pago

El handshake TLS ocurre **antes** de enviar la petición, así que si falla, GestoPago nunca recibió la transacción. Por eso un fallo de TLS no se trata como un timeout:

| Situación | Resultado | Saldo del cliente |
|---|---|---|
| Falla el handshake en sendTx | HTTP 502, código 60 | Se libera (la operación nunca salió) |
| Falla el handshake en confirmTx | HTTP 502, código 60 | Sigue retenido (el resultado del sendTx original sigue sin conocerse) |
| Timeout después de enviar | HTTP 504, pendiente de confirmTx | Sigue retenido hasta conciliar |

Al cliente solo se le responde "No fue posible establecer un canal seguro con el procesador de pagos". El detalle técnico del certificado va únicamente al log.

---

## 2. HTTPS de nuestra API

### En Render (producción)

Render termina el HTTPS en su proxy con un certificado público y reenvía la petición al contenedor como HTTP. Con `server.forward-headers-strategy=native`, Spring reconoce que la petición original fue HTTPS y agrega la cabecera `Strict-Transport-Security` (HSTS), que obliga al navegador a usar siempre HTTPS.

### En local (perfil `https`)

```bash
bash scripts/generar-certificado-tls.sh
```

```bash
SPRING_PROFILES_ACTIVE=https API_TLS_PASSWORD='<la contraseña que eligió>' java -jar build/libs/prueba-1.0.jar
```

```bash
curl --cacert certs/api-tls.crt https://localhost:8443/actuator/health
```

- El script genera un certificado RSA 3072 autofirmado con nombres `localhost`, `127.0.0.1` y `::1`, uso exclusivo de servidor y vigencia de 365 días.
- La **llave privada** (`certs/api-tls.p12`) nunca se versiona: `certs/` está en `.gitignore`.
- La contraseña del almacén solo llega por la variable `API_TLS_PASSWORD`. Sin ella la aplicación no arranca, en lugar de usar una contraseña por defecto. El script tampoco la pasa como argumento visible, sino por variable de entorno.
- También aquí solo se aceptan TLS 1.3 y 1.2.

---

## 3. Otros riesgos atendidos junto con el TLS

- **Bloqueo de IP por el catálogo.** La documentación de GestoPago bloquea la IP si `getProductList.do` se llama más de 3 veces al día. Antes, cada `GET /api/v1/productos` lo llamaba. Ahora se consulta como máximo una vez cada 12 horas (`CATALOGO_INTERVALO_HORAS`) y el resto se atiende desde la base de datos. Un intento fallido también cuenta. Las peticiones simultáneas generan una sola llamada.
- **Contraseña en los logs.** GestoPago recibe la contraseña del distribuidor en la URL (`?password=...`), y los errores de Feign incluyen esa URL. `OcultadorDatosSensibles` la reemplaza por `****` antes de escribir el log (también tokens Bearer, api keys y el payload `signed`).

---

## 4. Cómo se verificó

| Prueba | Resultado |
|---|---|
| `CanalTlsGestoPagoTest`: servidor TLS local con certificado autofirmado | Rechazado con `SSLHandshakeException` |
| Mismo servidor, truststore que sí confía en él | Handshake TLS 1.3 correcto |
| Servidor que solo ofrece TLS 1.2, cliente que solo permite 1.3 | Rechazado (no se degrada) |
| Feign contra el servidor no confiable | Falla en el handshake, antes de enviar la petición |
| Handshake real contra `gestopago.portalventas.net` | TLS 1.3, emitido por RapidSSL/DigiCert, aceptado |
| Handshake real contra `www.google.com` (otra CA) | Rechazado: el truststore no confía en esa CA |
| API local en HTTPS con `curl --cacert` | 200, certificado verificado |
| API local sin confiar en el certificado | `curl` rechaza la conexión (código 60) |
| TLS 1.1 forzado contra la API local | El servidor responde con la alerta 70 (protocol version) |
| Cabeceras de la API en HTTPS | HSTS, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` |
