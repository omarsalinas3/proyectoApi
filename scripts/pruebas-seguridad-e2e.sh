#!/usr/bin/env bash
# Pruebas de punta a punta de la API: estructura uniforme de respuestas, validaciones con @Valid,
# búsqueda dinámica con filtros combinables, campos controlados por el servidor, seguridad y manejo de errores.
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

# Identificadores únicos por ejecución: permiten correr las pruebas sobre una base que ya tiene datos.
read -r ANA_CURP ANA_RFC ANA_CORREO ANA_FACEID JORGE_CURP JORGE_RFC JORGE_CORREO JORGE_USER <<< "$(node -e "
const c=require('crypto');const r=(a,n)=>Array.from({length:n},()=>a[c.randomInt(a.length)]).join('');
const CONS='BCDFGHJKLMNPQRSTVWXYZ',ALNUM='ABCDEFGHIJKLMNPQRSTUVWXYZ0123456789',DIG='0123456789';
const suf=r('abcdefghijkmnpqrstuvwxyz23456789',6);
console.log(['MOCA920730MDF'+r(CONS,3)+r(ALNUM,1)+r(DIG,1),'MOCA920730'+r(ALNUM,3),'ana.'+suf+'@example.com',
  r('123456789',1)+r(DIG,11),'RASJ850112HPL'+r(CONS,3)+r(ALNUM,1)+r(DIG,1),'RASJ850112'+r(ALNUM,3),
  'jorge.'+suf+'@example.com','jorge.'+suf].join(' '))")"
echo "Datos de prueba: ana=$ANA_CURP / $ANA_CORREO | jorge=$JORGE_CURP / $JORGE_USER"

# J <expresión>: evalúa una expresión sobre el JSON de la última respuesta (ej. J datos.id)
J() { node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{try{const o=JSON.parse(d);const v=eval('o.'+process.argv[1]);console.log(v===undefined||v===null?'':(typeof v==='object'?JSON.stringify(v):v))}catch(e){console.log('')}})" "$1" < "$TMP/e2e_body"; }

# check <descripción> <status esperado> <método> <ruta> [token] [cuerpo]
check() {
  local desc=$1 exp=$2 m=$3 p=$4 tok=$5 body=$6
  local args=(-s -o "$TMP/e2e_body" -w '%{http_code}' -X "$m" "$B$p")
  [ -n "$tok" ] && args+=(-H "Authorization: Bearer $tok")
  if [ -n "$body" ]; then printf '%s' "$body" > "$TMP/e2e_req.json"; args+=(-H 'Content-Type: application/json' --data-binary "@$(ruta_curl "$TMP/e2e_req.json")"); fi
  local code; code=$(curl "${args[@]}")
  if [ "$code" = "$exp" ]; then PASS=$((PASS+1)); printf 'OK    %s  %-6s %-30s %s\n' "$code" "$m" "$p" "$desc"
  else FAIL=$((FAIL+1)); printf 'FALLA %s (esperado %s) %s %s %s\n      %s\n' "$code" "$exp" "$m" "$p" "$desc" "$(head -c 500 "$TMP/e2e_body")"; fi
  if [ "$code" -ge 400 ] 2>/dev/null; then
    local det err; det=$(J detalle); err=$(J errores)
    [ -n "$det" ] && printf '        detalle: %s\n' "$det"
    [ -n "$err" ] && printf '        errores: %s\n' "$err"
  fi
}

# expect <descripción> <valor obtenido> <valor esperado>
expect() {
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); printf 'OK    %s = %s\n' "$1" "$2"
  else FAIL=$((FAIL+1)); printf 'FALLA %s: se obtuvo "%s", se esperaba "%s"\n' "$1" "$2" "$3"; fi
}

ANA='{"bandera":1,"datosPersonales":{"nombre":"Ana","segundoNombre":"Sofía","apellidoPaterno":"Morales","apellidoMaterno":"Castillo","fechaNacimiento":"1992-07-30","curp":"'"$ANA_CURP"'","rfc":"'"$ANA_RFC"'","sexo":"F","nacionalidad":"Mexicana","estadoCivil":"SOLTERO","datosBiometricos":'"$ANA_FACEID"'},"datosContacto":{"correo":"'"$ANA_CORREO"'","telefonoMovil":"5544332211"},"domicilio":{"calle":"Av. Insurgentes Sur","numeroExterior":"1602","colonia":"Crédito Constructor","municipio":"Benito Juárez","estado":"Ciudad de México","codigoPostal":"03940","pais":"México"},"informacionFinanciera":{"ocupacion":"Arquitecta","ingresoMensual":48000.75},"autenticacion":{"password":"AnaSegura#2026","faceId":'"$ANA_FACEID"'}}'
JORGE='{"nombre":"Jorge","apellidoPaterno":"Ramírez","apellidoMaterno":"Salgado","fechaNacimiento":"1985-01-12","curp":"'"$JORGE_CURP"'","rfc":"'"$JORGE_RFC"'","sexo":"M","nacionalidad":"Mexicana","estadoCivil":"CASADO","correo":"'"$JORGE_CORREO"'","telefonoMovil":"2223344556","ocupacion":"Médico Cirujano","empresa":"Hospital Ángeles Puebla","ingresoMensual":85000,"domicilio":{"calle":"Blvd. del Niño Poblano","numeroExterior":"2510","colonia":"Reserva Territorial","municipio":"San Andrés Cholula","estado":"Puebla","codigoPostal":"72810","pais":"México"},"username":"'"$JORGE_USER"'","password":"Jorge$Pass2026"}'

echo "== Registro (público) y estructura uniforme de éxito"
check "alta layaway bandera 1" 201 POST /api/v1/layaway/cliente "" "$ANA"
ANA_ID=$(J datos.id); ANA_CTA=$(J datos.cuenta.numeroCuenta)
expect "respuesta de éxito: codigo" "$(J codigo)" "0"
expect "respuesta de éxito: codigoRespuesta" "$(J codigoRespuesta)" "EXITO"
check "alta POST /clientes" 201 POST /api/v1/clientes "" "$JORGE"
JORGE_ID=$(J datos.id); JORGE_CTA=$(J datos.cuenta.numeroCuenta)
echo "   ana id=$ANA_ID cuenta=$ANA_CTA | jorge id=$JORGE_ID cuenta=$JORGE_CTA"
check "alta duplicada (misma CURP)" 409 POST /api/v1/clientes "" "$JORGE"
check "alta con campos controlados por el servidor" 400 POST /api/v1/clientes "" "${JORGE%\}},\"activo\":false,\"numeroCuenta\":\"1111222233334444\"}"
check "alta con datos inválidos (errores por campo)" 400 POST /api/v1/clientes "" '{"nombre":"J0rge","curp":"XYZ","correo":"no-es-correo","telefonoMovil":"123"}'
expect "error de validación: codigoRespuesta" "$(J codigoRespuesta)" "ERROR_PARAMETRO_INVALIDO"
expect "error de validación: motivo de la CURP" "$(J 'errores.curp.includes("CURP")')" "true"
expect "error de validación: incluye correo y teléfono" "$(J 'errores.correo!==undefined && o.errores.telefonoMovil!==undefined')" "true"
check "alta con cuerpo vacío" 400 POST /api/v1/clientes "" " "
check "alta con JSON mal formado" 400 POST /api/v1/clientes "" '{"nombre":'
check "alta con tipo de dato inválido" 400 POST /api/v1/clientes "" '{"ingresoMensual":"mucho"}'

echo "== Login"
check "login ana (correo + contraseña)" 200 POST /api/v1/auth/login "" '{"bandera":1,"username":"'"$ANA_CORREO"'","password":"AnaSegura#2026"}'
TA=$(J datos.token)
check "login jorge (username)" 200 POST /api/v1/auth/login "" '{"bandera":1,"username":"'"$JORGE_USER"'","password":"Jorge$Pass2026"}'
TJ=$(J datos.token)
check "login ejecutivo" 200 POST /api/v1/auth/login "" "{\"bandera\":1,\"username\":\"$EJECUTIVO_USUARIO\",\"password\":\"$EJECUTIVO_PASSWORD\"}"
TE=$(J datos.token)
check "contraseña incorrecta" 401 POST /api/v1/auth/login "" '{"bandera":1,"username":"'"$ANA_CORREO"'","password":"Mala#2026x"}'
check "bandera inexistente" 400 POST /api/v1/auth/login "" '{"bandera":9,"username":"'"$ANA_CORREO"'","password":"x"}'
check "face id correcto" 200 POST /api/v1/auth/login "" '{"bandera":2,"username":"'"$ANA_CORREO"'","faceIdBiometrico":'"$ANA_FACEID"'}'
TA=$(J datos.token)

echo "== Búsqueda dinámica (único endpoint, filtros en el cuerpo)"
check "sin token" 401 POST /api/v1/clientes/buscar "" '{}'
check "ejecutivo: todos" 200 POST /api/v1/clientes/buscar "$TE" '{}'
expect "total de clientes (incluye los 2 de prueba)" "$(J 'datos.totalElementos>=2')" "true"
check "ejecutivo: por CURP" 200 POST /api/v1/clientes/buscar "$TE" '{"curp":"'"$JORGE_CURP"'"}'
expect "cliente encontrado por CURP" "$(J 'datos.contenido[0].id')" "$JORGE_ID"
check "ejecutivo: por RFC + activo" 200 POST /api/v1/clientes/buscar "$TE" '{"rfc":"'"$ANA_RFC"'","activo":true}'
expect "cliente encontrado por RFC" "$(J 'datos.contenido[0].id')" "$ANA_ID"
check "ejecutivo: por correo" 200 POST /api/v1/clientes/buscar "$TE" '{"correo":"'"$JORGE_CORREO"'"}'
expect "cliente encontrado por correo" "$(J datos.totalElementos)" "1"
check "ejecutivo: por número de cuenta" 200 POST /api/v1/clientes/buscar "$TE" "{\"numeroCuenta\":\"$ANA_CTA\"}"
expect "cliente encontrado por cuenta" "$(J 'datos.contenido[0].id')" "$ANA_ID"
check "ejecutivo: por nombre parcial" 200 POST /api/v1/clientes/buscar "$TE" '{"nombre":"ramír","tamanio":100}'
expect "cliente encontrado por nombre" "$(J "datos.contenido.some(c=>c.id==$JORGE_ID)")" "true"
HOY=$(date +%F)
check "ejecutivo: rango de fechas + paginación" 200 POST /api/v1/clientes/buscar "$TE" "{\"fechaRegistroDesde\":\"2026-01-01\",\"fechaRegistroHasta\":\"$HOY\",\"pagina\":0,\"tamanio\":1}"
expect "paginación: elementos por página" "$(J 'datos.contenido.length')" "1"
expect "paginación: una página por cliente" "$(J 'datos.totalPaginas===o.datos.totalElementos && o.datos.totalElementos>=2')" "true"
check "ejecutivo: filtros combinados sin coincidencias" 200 POST /api/v1/clientes/buscar "$TE" '{"curp":"'"$JORGE_CURP"'","rfc":"'"$ANA_RFC"'"}'
expect "sin coincidencias: mensaje" "$(J mensaje)" "No se encontraron clientes con los criterios indicados"
check "filtros inválidos" 400 POST /api/v1/clientes/buscar "$TE" '{"id":0,"curp":"XYZ","numeroCuenta":"12ab","tamanio":500}'
check "rango de fechas invertido" 400 POST /api/v1/clientes/buscar "$TE" '{"fechaRegistroDesde":"2026-06-30","fechaRegistroHasta":"2026-01-01"}'
check "fecha con formato inválido" 400 POST /api/v1/clientes/buscar "$TE" '{"fechaRegistroDesde":"2026-13-45"}'
check "filtro inexistente" 400 POST /api/v1/clientes/buscar "$TE" '{"edad":30}'
check "cliente: búsqueda sin filtros = solo su registro" 200 POST /api/v1/clientes/buscar "$TA" '{}'
expect "cliente solo ve su registro" "$(J datos.totalElementos):$(J 'datos.contenido[0].id')" "1:$ANA_ID"
check "cliente: busca el id de otro cliente" 403 POST /api/v1/clientes/buscar "$TA" "{\"id\":$JORGE_ID}"
check "cliente: busca la CURP de otro cliente (sin resultados)" 200 POST /api/v1/clientes/buscar "$TA" '{"curp":"'"$JORGE_CURP"'"}'
expect "no ve datos de otro cliente" "$(J datos.totalElementos)" "0"

echo "== Cuentas"
check "ejecutivo: cuentas activas" 200 POST /api/v1/cuentas/buscar "$TE" '{"estatus":"ACTIVA"}'
expect "cuentas activas (incluye las 2 de prueba)" "$(J 'datos.totalElementos>=2')" "true"
check "cliente: sus cuentas" 200 POST /api/v1/cuentas/buscar "$TA" '{}'
expect "cliente solo ve su cuenta" "$(J 'datos.contenido[0].cuenta.numeroCuenta')" "$ANA_CTA"
check "cliente: cuentas de otro cliente" 403 POST /api/v1/cuentas/buscar "$TA" "{\"clienteId\":$JORGE_ID}"
check "estatus fuera de catálogo" 400 POST /api/v1/cuentas/buscar "$TE" '{"estatus":"BORRADA"}'
check "cliente: saldo propio" 200 POST /api/v1/cuentas/saldo "$TA" "{\"numeroCuenta\":\"$ANA_CTA\"}"
check "cliente: saldo de otro cliente" 403 POST /api/v1/cuentas/saldo "$TA" "{\"numeroCuenta\":\"$JORGE_CTA\"}"
check "ejecutivo: saldo de cuenta inexistente" 404 POST /api/v1/cuentas/saldo "$TE" '{"numeroCuenta":"4152000000000000"}'
check "saldo sin número de cuenta" 400 POST /api/v1/cuentas/saldo "$TE" '{}'

echo "== Actualización (id en el cuerpo, campos del servidor protegidos)"
UPD="{\"id\":$ANA_ID,\"nombre\":\"Ana\",\"apellidoPaterno\":\"Morales\",\"apellidoMaterno\":\"Castillo\",\"sexo\":\"F\",\"nacionalidad\":\"Mexicana\",\"estadoCivil\":\"CASADO\",\"correo\":\"$ANA_CORREO\",\"telefonoMovil\":\"5599887766\",\"ocupacion\":\"Arquitecta\",\"empresa\":\"Constructora\",\"ingresoMensual\":45000,\"domicilio\":{\"calle\":\"Av Patria\",\"numeroExterior\":\"2200\",\"colonia\":\"Centro\",\"municipio\":\"Zapopan\",\"estado\":\"Jalisco\",\"codigoPostal\":\"45110\",\"pais\":\"Mexico\"}}"
check "cliente actualiza su registro" 200 PUT /api/v1/clientes "$TA" "$UPD"
expect "estado civil actualizado" "$(J datos.estadoCivil)" "CASADO"
check "intenta modificar CURP/RFC/activo" 400 PUT /api/v1/clientes "$TA" "${UPD%\}},\"curp\":\"XXXX000000HDFXXX00\",\"rfc\":\"XXXX000000XX0\",\"activo\":false}"
check "actualización sin id" 400 PUT /api/v1/clientes "$TA" '{"nombre":"Ana"}'
check "cliente actualiza a otro cliente" 403 PUT /api/v1/clientes "$TJ" "$UPD"
check "ejecutivo actualiza cliente inexistente" 404 PUT /api/v1/clientes "$TE" "${UPD/\"id\":$ANA_ID/\"id\":999999999}"
check "correo de otro cliente" 409 PUT /api/v1/clientes "$TA" "${UPD/$ANA_CORREO/$JORGE_CORREO}"

echo "== Layaway banderas 2 y 3"
check "bandera 2 sin token" 401 POST /api/v1/layaway/cliente "" "{\"bandera\":2,\"clienteId\":$ANA_ID,\"datosContacto\":{\"telefonoMovil\":\"5599887766\"}}"
check "bandera 2 sin clienteId" 400 POST /api/v1/layaway/cliente "$TA" '{"bandera":2}'
check "bandera 2 a otro cliente" 403 POST /api/v1/layaway/cliente "$TA" "{\"bandera\":2,\"clienteId\":$JORGE_ID}"
check "bandera 2 propia" 200 POST /api/v1/layaway/cliente "$TA" "{\"bandera\":2,\"clienteId\":$ANA_ID,\"datosContacto\":{\"telefonoMovil\":\"5511112222\"}}"
check "bandera sin valor" 400 POST /api/v1/layaway/cliente "" '{}'

echo "== Sesión y baja lógica"
check "verificar sesión (sin cuerpo)" 200 POST /api/v1/auth/sesion/verificar "$TA"
check "verificar sesión con timeout" 200 POST /api/v1/auth/sesion/verificar "$TA" '{"timeoutDinamico":600}'
check "timeout inválido" 400 POST /api/v1/auth/sesion/verificar "$TA" '{"timeoutDinamico":-5}'
check "logout de jorge" 200 POST /api/v1/auth/logout "$TJ"
check "token de jorge tras logout" 401 POST /api/v1/clientes/buscar "$TJ" '{}'
check "baja de otro cliente" 403 DELETE /api/v1/clientes "$TA" "{\"id\":$JORGE_ID}"
check "baja con id inválido" 400 DELETE /api/v1/clientes "$TE" '{"id":-1}'
check "ejecutivo da de baja a jorge" 200 DELETE /api/v1/clientes "$TE" "{\"id\":$JORGE_ID}"
expect "cliente inactivo" "$(J datos.activo)" "false"
check "ejecutivo: jorge ya no aparece entre los activos" 200 POST /api/v1/clientes/buscar "$TE" '{"curp":"'"$JORGE_CURP"'","activo":true}'
expect "jorge activo" "$(J datos.totalElementos)" "0"
check "ejecutivo: cuentas canceladas de jorge" 200 POST /api/v1/cuentas/buscar "$TE" "{\"estatus\":\"CANCELADA\",\"clienteId\":$JORGE_ID}"
expect "la cuenta de jorge quedó cancelada" "$(J 'datos.contenido[0].cuenta.numeroCuenta')" "$JORGE_CTA"
check "jorge dado de baja no inicia sesión" 401 POST /api/v1/auth/login "" '{"bandera":1,"username":"'"$JORGE_USER"'","password":"Jorge$Pass2026"}'

echo "== Errores generales y configuración"
check "ruta inexistente" 404 GET / "$TE"
check "método no permitido" 405 GET /api/v1/clientes/buscar "$TE"
check "Content-Type no soportado" 415 POST /api/v1/auth/login "" ""
check "health público" 200 GET /actuator/health
check "actuator env no expuesto" 404 GET /actuator/env "$TE"
check "swagger público" 200 GET /v3/api-docs

echo
echo "RESULTADO: $PASS OK, $FAIL fallas"
[ "$FAIL" -eq 0 ]
