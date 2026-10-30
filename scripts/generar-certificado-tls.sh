#!/usr/bin/env bash
# Genera el certificado TLS de la API para correrla en HTTPS en local (perfil "https").
#
#   bash scripts/generar-certificado-tls.sh [nombre-dns-extra ...]
#
# Crea en certs/ (carpeta ignorada por git):
#   api-tls.p12  almacén PKCS12 con la llave privada y el certificado (NUNCA se versiona ni se comparte)
#   api-tls.crt  certificado público en PEM, para que los clientes confíen en él (curl --cacert certs/api-tls.crt)
#
# La contraseña del almacén se toma de API_TLS_PASSWORD; si no existe, se pide sin mostrarla en pantalla.
# Es un certificado autofirmado para desarrollo: en Render el HTTPS lo pone su proxy con un certificado público.
set -euo pipefail

DIR="$(cd "$(dirname "$0")/.." && pwd)/certs"
ALIAS="proyecto-api"
DIAS="${API_TLS_DIAS:-365}"

if ! command -v keytool >/dev/null 2>&1; then
  echo "No se encontró keytool: instale un JDK 21 y agréguelo al PATH" >&2
  exit 1
fi

if [ -z "${API_TLS_PASSWORD:-}" ]; then
  read -rsp "Contraseña para el almacén (mínimo 8 caracteres): " API_TLS_PASSWORD; echo
fi
if [ "${#API_TLS_PASSWORD}" -lt 8 ]; then
  echo "La contraseña debe tener al menos 8 caracteres" >&2
  exit 1
fi

SAN="dns:localhost,ip:127.0.0.1,ip:::1"
for extra in "$@"; do SAN="$SAN,dns:$extra"; done

mkdir -p "$DIR"
if [ -f "$DIR/api-tls.p12" ]; then
  respaldo="$DIR/api-tls.p12.$(date +%Y%m%d%H%M%S).bak"
  mv "$DIR/api-tls.p12" "$respaldo"
  echo "Ya existía un certificado; se respaldó en $respaldo"
fi

# La contraseña viaja por variable de entorno (:env), no como argumento visible en la lista de procesos
export API_TLS_PASSWORD
keytool -genkeypair -alias "$ALIAS" -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity "$DIAS" \
  -dname "CN=localhost, OU=Desarrollo, O=ProyectoApi, C=MX" -ext "SAN=$SAN" \
  -ext "KeyUsage=digitalSignature,keyEncipherment" -ext "ExtendedKeyUsage=serverAuth" \
  -storetype PKCS12 -keystore "$DIR/api-tls.p12" -storepass:env API_TLS_PASSWORD -keypass:env API_TLS_PASSWORD

keytool -exportcert -rfc -alias "$ALIAS" -keystore "$DIR/api-tls.p12" -storetype PKCS12 \
  -storepass:env API_TLS_PASSWORD -file "$DIR/api-tls.crt"
chmod 600 "$DIR/api-tls.p12" 2>/dev/null || true

echo
echo "Certificado generado (vigencia $DIAS días, nombres: $SAN)"
keytool -printcert -file "$DIR/api-tls.crt" | grep -E "Propietario|Owner|Válido|Valid|SHA256:" || true
echo
echo "Arranque en HTTPS (puerto 8443) con:"
echo "  SPRING_PROFILES_ACTIVE=https API_TLS_PASSWORD=... java -jar build/libs/prueba-1.0.jar"
echo "Pruebe con:"
echo "  curl --cacert certs/api-tls.crt https://localhost:8443/actuator/health"
