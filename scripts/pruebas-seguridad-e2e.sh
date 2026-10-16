#!/usr/bin/env bash
# Pruebas de punta a punta de seguridad, validación de identificadores y manejo de errores.
#
# Requisitos: API levantada con base de datos vacía, Node.js y curl (probado en Git Bash y Linux).
# Uso:
#   EJECUTIVO_PASSWORD='<clave del ejecutivo>' B=http://localhost:8080 bash scripts/pruebas-seguridad-e2e.sh
#
# Para levantar la API localmente con H2 en memoria:
#   SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect \
#   DB_URL='jdbc:h2:mem:e2e;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1' \
#   DB_DRIVER=org.h2.Driver DB_USERNAME=sa DB_PASSWORD= EJECUTIVO_PASSWORD='<clave>' \
#   JWT_SECRET='<al menos 32 caracteres>' java -jar build/libs/prueba-1.0.jar
B=${B:-http://localhost:8080}
: "${EJECUTIVO_PASSWORD:?Defina EJECUTIVO_PASSWORD con la clave configurada en la API}"
EJECUTIVO_USUARIO=${EJECUTIVO_USUARIO:-ejecutivo}
TMP=${TMPDIR:-/tmp}
ruta_curl() { if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else echo "$1"; fi; }
PASS=0; FAIL=0
J() { node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{try{const o=JSON.parse(d);console.log(eval('o.'+process.argv[1])??'')}catch(e){console.log('')}})" "$1"; }

# check <descripcion> <status esperado> <metodo> <ruta> [token] [body]
check() {
  local desc=$1 exp=$2 m=$3 p=$4 tok=$5 body=$6
  local args=(-s -o "$TMP/e2e_body" -w '%{http_code}' -X "$m" "$B$p")
  [ -n "$tok" ] && args+=(-H "Authorization: Bearer $tok")
  if [ -n "$body" ]; then printf '%s' "$body" > "$TMP/e2e_req.json"; args+=(-H 'Content-Type: application/json' --data-binary "@$(ruta_curl "$TMP/e2e_req.json")"); fi
  local code; code=$(curl "${args[@]}")
  local det; det=$(J detalle < "$TMP/e2e_body")
  if [ "$code" = "$exp" ]; then PASS=$((PASS+1)); printf 'OK    %s  %-6s %-48s %s\n' "$code" "$m" "$p" "$desc"
  else FAIL=$((FAIL+1)); printf 'FALLA %s (esperado %s) %s %s %s\n      %s\n' "$code" "$exp" "$m" "$p" "$desc" "$(head -c 400 "$TMP/e2e_body")"; fi
  [ -n "$det" ] && [ "$code" != "200" ] && [ "$code" != "201" ] && printf '        detalle: %s\n' "$det"
}

ANA='{"bandera":1,"datosPersonales":{"nombre":"Ana","segundoNombre":"Sofía","apellidoPaterno":"Morales","apellidoMaterno":"Castillo","fechaNacimiento":"1992-07-30","curp":"MOCA920730MDFRLSA7","rfc":"MOCA920730QW4","sexo":"F","nacionalidad":"Mexicana","estadoCivil":"SOLTERO","datosBiometricos":987654321098},"datosContacto":{"correo":"ana.morales@example.com","telefonoMovil":"5544332211"},"domicilio":{"calle":"Av. Insurgentes Sur","numeroExterior":"1602","colonia":"Crédito Constructor","municipio":"Benito Juárez","estado":"Ciudad de México","codigoPostal":"03940","pais":"México"},"informacionFinanciera":{"ocupacion":"Arquitecta","ingresoMensual":48000.75},"autenticacion":{"password":"AnaSegura#2026","faceId":987654321098}}'
JORGE='{"nombre":"Jorge","apellidoPaterno":"Ramírez","apellidoMaterno":"Salgado","fechaNacimiento":"1985-01-12","curp":"RASJ850112HPLMLR04","rfc":"RASJ850112HB6","sexo":"M","nacionalidad":"Mexicana","estadoCivil":"CASADO","correo":"jorge.ramirez@example.com","telefonoMovil":"2223344556","ocupacion":"Médico Cirujano","empresa":"Hospital Ángeles Puebla","ingresoMensual":85000,"domicilio":{"calle":"Blvd. del Niño Poblano","numeroExterior":"2510","colonia":"Reserva Territorial","municipio":"San Andrés Cholula","estado":"Puebla","codigoPostal":"72810","pais":"México"},"username":"jorge.ramirez85","password":"Jorge$Pass2026"}'

echo "== Registro (público)"
check "alta layaway bandera 1 sin token" 200 POST /api/v1/layaway/cliente "" "$ANA"
ANA_ID=$(J id < "$TMP/e2e_body"); ANA_CTA=$(J cuenta.numeroCuenta < "$TMP/e2e_body")
check "alta POST /clientes sin token" 201 POST /api/v1/clientes "" "$JORGE"
JORGE_ID=$(J id < "$TMP/e2e_body"); JORGE_CTA=$(J cuenta.numeroCuenta < "$TMP/e2e_body")
echo "   ana id=$ANA_ID cuenta=$ANA_CTA | jorge id=$JORGE_ID cuenta=$JORGE_CTA"

echo "== Login"
check "login ana (correo + contraseña)" 200 POST /api/v1/auth/login "" '{"bandera":1,"username":"ana.morales@example.com","password":"AnaSegura#2026"}'
TA=$(J token < "$TMP/e2e_body"); echo "   rol=$(J rol < "$TMP/e2e_body") tipo=$(J tipoToken < "$TMP/e2e_body") expira=$(J expiraEnSegundos < "$TMP/e2e_body")"
check "login jorge (username)" 200 POST /api/v1/auth/login "" '{"bandera":1,"username":"jorge.ramirez85","password":"Jorge$Pass2026"}'
TJ=$(J token < "$TMP/e2e_body")
check "login ejecutivo" 200 POST /api/v1/auth/login "" "{\"bandera\":1,\"username\":\"$EJECUTIVO_USUARIO\",\"password\":\"$EJECUTIVO_PASSWORD\"}"
TE=$(J token < "$TMP/e2e_body")
check "contraseña incorrecta" 401 POST /api/v1/auth/login "" '{"bandera":1,"username":"ana.morales@example.com","password":"Mala#2026x"}'
check "usuario inexistente (mismo mensaje)" 401 POST /api/v1/auth/login "" '{"bandera":1,"username":"nadie@example.com","password":"Mala#2026x"}'
check "face id correcto" 200 POST /api/v1/auth/login "" '{"bandera":2,"username":"ana.morales@example.com","faceIdBiometrico":987654321098}'
TA=$(J token < "$TMP/e2e_body")
check "face id sin biometría registrada (jorge)" 401 POST /api/v1/auth/login "" '{"bandera":2,"username":"jorge.ramirez85","faceIdBiometrico":1}'

echo "== Consulta por ID: autenticación"
check "sin token" 401 GET /api/v1/clientes/$ANA_ID
check "token alterado" 401 GET /api/v1/clientes/$ANA_ID "${TA}x"
check "token basura" 401 GET /api/v1/clientes/$ANA_ID "abc.def.ghi"

echo "== Consulta por ID: identificadores inválidos (antes de la lógica de negocio)"
check "id texto" 400 GET /api/v1/clientes/abc "$TA"
check "id cero" 400 GET /api/v1/clientes/0 "$TA"
check "id negativo" 400 GET /api/v1/clientes/-5 "$TA"
check "id decimal" 400 GET /api/v1/clientes/1.5 "$TA"
check "id fuera de rango Long" 400 GET /api/v1/clientes/99999999999999999999 "$TA"

echo "== Consulta por ID: autorización por registro (IDOR)"
check "ana consulta su propio id" 200 GET /api/v1/clientes/$ANA_ID "$TA"
check "ana consulta el id de jorge" 403 GET /api/v1/clientes/$JORGE_ID "$TA"
check "ana consulta un id inexistente (no revela existencia)" 403 GET /api/v1/clientes/9999 "$TA"
check "jorge consulta el id de ana" 403 GET /api/v1/clientes/$ANA_ID "$TJ"
check "ejecutivo consulta a ana" 200 GET /api/v1/clientes/$ANA_ID "$TE"
check "ejecutivo consulta id inexistente" 404 GET /api/v1/clientes/9999 "$TE"

echo "== Otras consultas por identificador"
check "ana por su CURP" 200 GET /api/v1/clientes/curp/MOCA920730MDFRLSA7 "$TA"
check "ana por CURP de jorge" 403 GET /api/v1/clientes/curp/RASJ850112HPLMLR04 "$TA"
check "CURP con formato inválido" 400 GET /api/v1/clientes/curp/XYZ "$TA"
check "ana por RFC de jorge" 403 GET /api/v1/clientes/rfc/RASJ850112HB6 "$TA"
check "ana por correo de jorge" 403 GET /api/v1/clientes/correo/jorge.ramirez@example.com "$TA"
check "ana por su cuenta" 200 GET /api/v1/clientes/cuenta/$ANA_CTA "$TA"
check "ana por cuenta de jorge" 403 GET /api/v1/clientes/cuenta/$JORGE_CTA "$TA"
check "cuenta con formato inválido" 400 GET /api/v1/clientes/cuenta/12ab "$TA"
check "ana: detalle de su cuenta" 200 GET /api/v1/cuentas/$ANA_CTA "$TA"
check "ana: saldo de su cuenta" 200 GET /api/v1/cuentas/$ANA_CTA/saldo "$TA"
check "ana: saldo de la cuenta de jorge" 403 GET /api/v1/cuentas/$JORGE_CTA/saldo "$TA"
check "ejecutivo: saldo de la cuenta de jorge" 200 GET /api/v1/cuentas/$JORGE_CTA/saldo "$TE"
check "ejecutivo: cuenta inexistente" 404 GET /api/v1/cuentas/4152000000000000 "$TE"

echo "== Consultas masivas (solo ejecutivo)"
check "cliente lista todos" 403 GET /api/v1/clientes "$TA"
check "cliente cuentas activas" 403 GET /api/v1/cuentas/activas "$TA"
check "cliente rango de fechas" 403 "GET" "/api/v1/clientes/rango-fechas?inicio=2026-01-01&fin=2026-12-31" "$TA"
check "ejecutivo lista todos" 200 GET /api/v1/clientes "$TE"
check "ejecutivo clientes activos" 200 GET "/api/v1/clientes?soloActivos=true" "$TE"
check "ejecutivo cuentas activas" 200 GET /api/v1/cuentas/activas "$TE"
check "ejecutivo rango válido" 200 GET "/api/v1/clientes/rango-fechas?inicio=2026-01-01&fin=2026-12-31" "$TE"
check "rango invertido" 400 GET "/api/v1/clientes/rango-fechas?inicio=2026-12-31&fin=2026-01-01" "$TE"
check "fecha mal formada" 400 GET "/api/v1/clientes/rango-fechas?inicio=2026-13-45&fin=2026-12-31" "$TE"
check "falta parámetro fin" 400 GET "/api/v1/clientes/rango-fechas?inicio=2026-01-01" "$TE"

echo "== Layaway banderas 2 y 3"
check "bandera 2 sin token" 401 POST /api/v1/layaway/cliente "" "{\"bandera\":2,\"clienteId\":$ANA_ID,\"datosContacto\":{\"telefonoMovil\":\"5599887766\"}}"
check "bandera 2 ana actualiza a jorge" 403 POST /api/v1/layaway/cliente "$TA" "{\"bandera\":2,\"clienteId\":$JORGE_ID,\"datosContacto\":{\"telefonoMovil\":\"5599887766\"}}"
check "bandera 2 clienteId inválido" 400 POST /api/v1/layaway/cliente "$TA" '{"bandera":2,"clienteId":0}'
check "bandera 2 ana se actualiza" 200 POST /api/v1/layaway/cliente "$TA" "{\"bandera\":2,\"clienteId\":$ANA_ID,\"datosContacto\":{\"telefonoMovil\":\"5599887766\"}}"
check "bandera 3 jorge da de baja a ana" 403 POST /api/v1/layaway/cliente "$TJ" "{\"bandera\":3,\"clienteId\":$ANA_ID}"
check "PUT /clientes con cuerpo vacío (se valida el cuerpo)" 400 PUT /api/v1/clientes/$ANA_ID "$TJ" '{}'
UPD='{"nombre":"Ana","apellidoPaterno":"Morales","apellidoMaterno":"Castillo","sexo":"F","nacionalidad":"Mexicana","estadoCivil":"CASADO","correo":"ana.otra@example.com","telefonoMovil":"5599887766","ocupacion":"Arquitecta","empresa":"Constructora","ingresoMensual":45000,"domicilio":{"calle":"Av Patria","numeroExterior":"2200","colonia":"Centro","municipio":"Zapopan","estado":"Jalisco","codigoPostal":"45110","pais":"Mexico"}}'
check "PUT /clientes de otro cliente" 403 PUT /api/v1/clientes/$ANA_ID "$TJ" "$UPD"
check "DELETE /clientes de otro cliente" 403 DELETE /api/v1/clientes/$ANA_ID "$TJ"

echo "== Sesión"
check "verificar propia sesión" 200 GET /api/v1/auth/sesion/verificar "$TA"
check "verificar sesión de otro usuario" 403 GET "/api/v1/auth/sesion/verificar?usuario=jorge.ramirez85" "$TA"
check "logout sin token" 401 POST /api/v1/auth/logout
check "logout de jorge" 200 POST /api/v1/auth/logout "$TJ"
check "URL con segmento vacío (firewall)" 400 GET /api/v1/cuentas//saldo "$TA"
check "token de jorge tras logout" 401 GET /api/v1/clientes/$JORGE_ID "$TJ"
check "baja lógica de jorge (ejecutivo)" 200 DELETE /api/v1/clientes/$JORGE_ID "$TE"
check "jorge dado de baja no puede iniciar sesión" 401 POST /api/v1/auth/login "" '{"bandera":1,"username":"jorge.ramirez85","password":"Jorge$Pass2026"}'

echo "== Errores generales y configuración"
check "ruta raíz inexistente" 404 GET / "$TE"
check "ruta inexistente sin token (no revela rutas)" 401 GET /api/v1/no-existe
check "método no permitido" 405 PATCH /api/v1/clientes/$ANA_ID "$TE"
check "JSON mal formado" 404 POST /api/v1/auth/login "" '{"bandera":"uno"}'
check "health público" 200 GET /actuator/health
check "actuator env no expuesto" 401 GET /actuator/env
check "actuator env con token" 404 GET /actuator/env "$TE"
check "swagger público" 200 GET /v3/api-docs

echo
echo "RESULTADO: $PASS OK, $FAIL fallas"
[ "$FAIL" -eq 0 ]
