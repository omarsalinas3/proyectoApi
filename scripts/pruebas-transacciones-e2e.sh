#!/usr/bin/env bash
# Pruebas de punta a punta del flujo de transacciones GestoPago (sendTx / confirmTx) contra el sandbox de GestoPago.
#
# Escenarios del sandbox (documentación PuntoRed):
#   - Recarga Telcel $10 (idProducto 582, idServicio 133), teléfono 5555555551 → CODIGO 01 (éxito)
#   - Mismo producto, teléfono 9999999991                                   → CODIGO 82 (timeout → confirmTx)
#   - Recarga Unefon $150 (idProducto 308, idServicio 116), teléfono 6569153326 → CODIGO 02 (saldo insuficiente)
#
# Requisitos: API levantada, Node.js y curl. Crea un cliente nuevo con datos aleatorios en cada corrida.
# Uso:
#   EJECUTIVO_PASSWORD='<clave del ejecutivo>' B=http://localhost:8080 bash scripts/pruebas-transacciones-e2e.sh
B=${B:-http://localhost:8080}
: "${EJECUTIVO_PASSWORD:?Defina EJECUTIVO_PASSWORD con la clave configurada en la API}"
EJECUTIVO_USUARIO=${EJECUTIVO_USUARIO:-ejecutivo}
TMP=${TMPDIR:-/tmp}
ruta_curl() { if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else echo "$1"; fi; }
PASS=0; FAIL=0

J() { node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{try{const o=JSON.parse(d);const v=eval('o.'+process.argv[1]);console.log(v===undefined||v===null?'':(typeof v==='object'?JSON.stringify(v):v))}catch(e){console.log('')}})" "$1" < "$TMP/e2e_body"; }

check() {
  local desc=$1 exp=$2 m=$3 p=$4 tok=$5 body=$6
  local args=(-s --max-time 120 -o "$TMP/e2e_body" -w '%{http_code}' -X "$m" "$B$p")
  [ -n "$tok" ] && args+=(-H "Authorization: Bearer $tok")
  if [ -n "$body" ]; then printf '%s' "$body" > "$TMP/e2e_req.json"; args+=(-H 'Content-Type: application/json' --data-binary "@$(ruta_curl "$TMP/e2e_req.json")"); fi
  local code; code=$(curl "${args[@]}")
  if [[ " $exp " == *" $code "* ]]; then PASS=$((PASS+1)); printf 'OK    %s  %-6s %-30s %s\n' "$code" "$m" "$p" "$desc"
  else FAIL=$((FAIL+1)); printf 'FALLA %s (esperado %s) %s %s %s\n      %s\n' "$code" "$exp" "$m" "$p" "$desc" "$(head -c 600 "$TMP/e2e_body")"; fi
  local det err; det=$(J detalle); err=$(J errores)
  [ -n "$det" ] && printf '        detalle: %s\n' "$det"
  [ -n "$err" ] && printf '        errores: %s\n' "$err"
}

expect() {
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); printf 'OK    %s = %s\n' "$1" "$2"
  else FAIL=$((FAIL+1)); printf 'FALLA %s: se obtuvo "%s", se esperaba "%s"\n' "$1" "$2" "$3"; fi
}

# Código TOTP (RFC 6238) para el secreto Base32. Como cada código se acepta una sola vez, usa el intervalo
# siguiente al último utilizado (la API tolera ±30 s) y espera si hace falta.
ULTIMO_PASO=0
totp() {
  local salida
  salida=$(node -e "
const c=require('crypto');const [sec,ult]=[process.argv[1],Number(process.argv[2])];
const A='ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';let bits='';for(const ch of sec)bits+=A.indexOf(ch).toString(2).padStart(5,'0');
const key=Buffer.from(bits.match(/.{8}/g).map(b=>parseInt(b,2)));
const ahora=Math.floor(Date.now()/1000/30);const paso=Math.max(ahora,ult+1);
if(paso>ahora+1){console.log('ESPERA '+((paso-1)*30*1000-Date.now()+500));process.exit(0)}
const buf=Buffer.alloc(8);buf.writeBigUInt64BE(BigInt(paso));const h=c.createHmac('sha1',key).update(buf).digest();
const o=h[h.length-1]&15;const n=((h[o]&127)<<24|h[o+1]<<16|h[o+2]<<8|h[o+3])%1000000;
console.log(paso+' '+String(n).padStart(6,'0'));" "$1" "$ULTIMO_PASO")
  if [[ $salida == ESPERA* ]]; then sleep $(( ${salida#ESPERA } / 1000 + 1 )); totp "$1"; return; fi
  ULTIMO_PASO=${salida%% *}; CODIGO=${salida##* }
}

read -r CURP RFC CORREO USUARIO <<< "$(node -e "
const c=require('crypto');const r=(a,n)=>Array.from({length:n},()=>a[c.randomInt(a.length)]).join('');
const suf=r('abcdefghijkmnpqrstuvwxyz23456789',6);
console.log(['LOGA880214HJC'+r('BCDFGHJKLMNPQRSTVWXYZ',3)+r('ABCDEFGHIJKLMNPQRSTUVWXYZ0123456789',1)+r('0123456789',1),
  'LOGA880214'+r('ABCDEFGHIJKLMNPQRSTUVWXYZ0123456789',3),'luis.'+suf+'@example.com','luis.'+suf].join(' '))")"
PASSWORD='LuisPago#2026'
echo "Cliente de prueba: $CURP / $USUARIO"

CLIENTE="{\"nombre\":\"Luis\",\"apellidoPaterno\":\"Lopez\",\"apellidoMaterno\":\"Garcia\",\"fechaNacimiento\":\"1988-02-14\",\"curp\":\"$CURP\",\"rfc\":\"$RFC\",\"sexo\":\"M\",\"nacionalidad\":\"Mexicana\",\"estadoCivil\":\"SOLTERO\",\"correo\":\"$CORREO\",\"telefonoMovil\":\"3312345678\",\"ocupacion\":\"Comerciante\",\"empresa\":\"Abarrotes Lopez\",\"ingresoMensual\":25000,\"domicilio\":{\"calle\":\"Av Vallarta\",\"numeroExterior\":\"100\",\"colonia\":\"Centro\",\"municipio\":\"Guadalajara\",\"estado\":\"Jalisco\",\"codigoPostal\":\"44100\",\"pais\":\"Mexico\"},\"username\":\"$USUARIO\",\"password\":\"$PASSWORD\"}"

echo "== Preparación"
check "registro del cliente" 201 POST /api/v1/clientes "" "$CLIENTE"
CTA=$(J datos.cuenta.numeroCuenta)
check "login del cliente" 200 POST /api/v1/auth/login "" "{\"bandera\":1,\"username\":\"$USUARIO\",\"password\":\"$PASSWORD\"}"
TC=$(J datos.token)
check "login del ejecutivo" 200 POST /api/v1/auth/login "" "{\"bandera\":1,\"username\":\"$EJECUTIVO_USUARIO\",\"password\":\"$EJECUTIVO_PASSWORD\"}"
TE=$(J datos.token)
check "catálogo de productos (GestoPago)" 200 GET /api/v1/productos "$TC"

RECARGA="{\"numCuenta\":\"$CTA\",\"idServicio\":133,\"idProducto\":582,\"monto\":10,\"telefono\":\"5555555551\""

echo "== Nodo «Saldo» y depósito"
check "transacción con saldo 0" 422 POST /api/v1/transacciones "$TC" "$RECARGA,\"codigo2fa\":\"000000\"}"
expect "código de saldo insuficiente" "$(J codigoRespuesta)" "ERROR_SALDO_INSUFICIENTE"
check "depósito hecho por un cliente" 403 POST /api/v1/cuentas/deposito "$TC" "{\"numeroCuenta\":\"$CTA\",\"monto\":500}"
check "depósito del ejecutivo" 200 POST /api/v1/cuentas/deposito "$TE" "{\"numeroCuenta\":\"$CTA\",\"monto\":500}"
expect "saldo disponible tras depósito" "$(J datos.saldo.saldoDisponible)" "500"

echo "== Nodo «2FA»"
check "transacción sin 2FA activado" 422 POST /api/v1/transacciones "$TC" "$RECARGA,\"codigo2fa\":\"000000\"}"
expect "código 2FA no configurado" "$(J codigoRespuesta)" "ERROR_2FA_NO_CONFIGURADO"
check "iniciar activación 2FA" 200 POST /api/v1/auth/2fa/activar "$TC"
SECRETO=$(J datos.secreto)
expect "URI otpauth generada" "$(J 'datos.uriProvisionamiento.startsWith("otpauth://totp/")')" "true"
check "confirmar 2FA con código incorrecto" 403 POST /api/v1/auth/2fa/confirmar "$TC" '{"codigo":"000000"}'
totp "$SECRETO"
check "confirmar 2FA con código correcto" 200 POST /api/v1/auth/2fa/confirmar "$TC" "{\"codigo\":\"$CODIGO\"}"
check "reactivar 2FA ya activo" 422 POST /api/v1/auth/2fa/activar "$TC"
check "transacción con 2FA incorrecto" 403 POST /api/v1/transacciones "$TC" "$RECARGA,\"codigo2fa\":\"000000\"}"

echo "== Nodo «Cliente Activo» y validaciones de entrada"
check "cuenta que no es del cliente" 403 POST /api/v1/transacciones "$TC" "{\"numCuenta\":\"4152000000000000\",\"idServicio\":133,\"idProducto\":582,\"monto\":10,\"telefono\":\"5555555551\",\"2fa\":\"123456\"}"
check "ejecutivo no puede enviar transacciones" 403 POST /api/v1/transacciones "$TE" "$RECARGA,\"codigo2fa\":\"123456\"}"
check "sin token" 401 POST /api/v1/transacciones "" "$RECARGA,\"codigo2fa\":\"123456\"}"
check "datos inválidos (longitudes y formatos)" 400 POST /api/v1/transacciones "$TC" '{"numCuenta":"12","idServicio":0,"idProducto":-1,"monto":10.555,"telefono":"55-55","referencia":"con espacios","codigo2fa":"12"}'
check "sin teléfono ni referencia" 400 POST /api/v1/transacciones "$TC" "{\"numCuenta\":\"$CTA\",\"idServicio\":133,\"idProducto\":582,\"monto\":10,\"codigo2fa\":\"123456\"}"
check "campo controlado por el servidor (upc)" 400 POST /api/v1/transacciones "$TC" "$RECARGA,\"codigo2fa\":\"123456\",\"upc\":\"HACK\"}"

echo "== Nodo «Sen Tx» (reglas del catálogo)"
totp "$SECRETO"
check "producto de otro servicio" 422 POST /api/v1/transacciones "$TC" "{\"numCuenta\":\"$CTA\",\"idServicio\":1,\"idProducto\":582,\"monto\":10,\"telefono\":\"5555555551\",\"codigo2fa\":\"$CODIGO\"}"
totp "$SECRETO"
check "recarga con monto distinto a su precio" 422 POST /api/v1/transacciones "$TC" "{\"numCuenta\":\"$CTA\",\"idServicio\":133,\"idProducto\":582,\"monto\":15,\"telefono\":\"5555555551\",\"codigo2fa\":\"$CODIGO\"}"
totp "$SECRETO"
check "pago de servicio sin referencia" 422 POST /api/v1/transacciones "$TC" "{\"numCuenta\":\"$CTA\",\"idServicio\":76,\"idProducto\":205,\"monto\":100,\"telefono\":\"5555555551\",\"codigo2fa\":\"$CODIGO\"}"

echo "== Envío a GestoPago: recarga exitosa (CODIGO 01)"
totp "$SECRETO"
check "recarga Telcel \$10" 201 POST /api/v1/transacciones "$TC" "$RECARGA,\"2fa\":\"$CODIGO\"}"
expect "estatus" "$(J datos.estatus)" "EXITOSA"
expect "código de GestoPago" "$(J datos.codigoProveedor)" "01"
expect "saldo tras la recarga" "$(J datos.saldoDisponible)" "490"
echo "   upc=$(J datos.upc) autorizacion=$(J datos.numeroAutorizacion) idTx=$(J datos.idTransaccionProveedor)"
check "reutilizar el mismo código 2FA" 403 POST /api/v1/transacciones "$TC" "$RECARGA,\"2fa\":\"$CODIGO\"}"

echo "== Envío a GestoPago: rechazo del procesador (CODIGO 02, saldo insuficiente del distribuidor)"
totp "$SECRETO"
check "recarga Unefon \$150 (escenario de rechazo)" "422 201" POST /api/v1/transacciones "$TC" "{\"numCuenta\":\"$CTA\",\"idServicio\":116,\"idProducto\":308,\"monto\":150,\"telefono\":\"6569153326\",\"codigo2fa\":\"$CODIGO\"}"
if [ "$(J codigoRespuesta)" = "ERROR_TRANSACCION_RECHAZADA" ]; then
  expect "monto liberado (saldo sin cambios)" "$(curl -s -H "Authorization: Bearer $TC" -H 'Content-Type: application/json' -d "{\"numeroCuenta\":\"$CTA\"}" "$B/api/v1/cuentas/saldo" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>console.log(JSON.parse(d).datos.saldo.saldoDisponible))")" "490"
else
  echo "   (el sandbox aplicó la recarga: código $(J datos.codigoProveedor))"
fi

echo "== Envío a GestoPago: timeout (CODIGO 82) y confirmTx"
totp "$SECRETO"
check "recarga con escenario de timeout" "504 201" POST /api/v1/transacciones "$TC" "{\"numCuenta\":\"$CTA\",\"idServicio\":133,\"idProducto\":582,\"monto\":10,\"telefono\":\"9999999991\",\"codigo2fa\":\"$CODIGO\"}"
UPC=$(J errores.upc)
if [ -n "$UPC" ]; then
  expect "estatus pendiente" "$(J codigoRespuesta)" "ERROR_TRANSACCION_PENDIENTE"
  check "confirmar antes de 62 s" 422 POST /api/v1/transacciones/confirmar "$TC" "{\"upc\":\"$UPC\"}"
  check "confirmar UPC inexistente" 404 POST /api/v1/transacciones/confirmar "$TC" '{"upc":"TXNOEXIST0"}'
  echo "   esperando 63 s para confirmTx..."
  sleep 63
  check "confirmTx tras 62 s" "200 422 504" POST /api/v1/transacciones/confirmar "$TC" "{\"upc\":\"$UPC\"}"
  echo "   resultado de confirmTx: $(J codigoRespuesta) $(J datos.estatus) $(J datos.codigoProveedor)"
  check "confirmar una transacción ya resuelta" "422 504" POST /api/v1/transacciones/confirmar "$TC" "{\"upc\":\"$UPC\"}"
fi

echo "== Saldo final"
check "saldo de la cuenta" 200 POST /api/v1/cuentas/saldo "$TC" "{\"numeroCuenta\":\"$CTA\"}"
echo "   saldo: $(J datos.saldo)"

echo
echo "RESULTADO: $PASS OK, $FAIL fallas"
[ "$FAIL" -eq 0 ]
