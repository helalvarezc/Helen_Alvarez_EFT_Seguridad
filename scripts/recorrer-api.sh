#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE:-https://localhost}"
: "${ADMIN_PASSWORD:?Defina la variable ADMIN_PASSWORD con la clave del usuario admin}"

PROXY_ARGS=()
if [ -n "${ZAP_PROXY:-}" ]; then
  PROXY_ARGS=(-x "$ZAP_PROXY")
fi

llamar() {
  curl -sk ${PROXY_ARGS[@]+"${PROXY_ARGS[@]}"} "$@"
}

token_de() {
  llamar -X POST "$BASE/login" -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" | sed -E 's/.*"token":"([^"]+)".*/\1/'
}

enviar() {
  if [ -n "${4:-}" ]; then
    llamar -X "$1" "$BASE$2" -H "Authorization: Bearer $3" -H "Content-Type: application/json" -d "$4"
  else
    llamar -X "$1" "$BASE$2" -H "Authorization: Bearer $3"
  fi
}

estado() {
  local args=()
  if [ -n "${3:-}" ]; then
    args=(-H "Authorization: Bearer $3")
  fi
  printf '%-7s %-34s -> ' "$1" "$2"
  llamar -o /dev/null -w '%{http_code}\n' -X "$1" "$BASE$2" ${args[@]+"${args[@]}"}
}

identificador() {
  local valor
  valor="$(sed -E 's/^\{"id":([0-9]+).*/\1/')"
  if ! [[ "$valor" =~ ^[0-9]+$ ]]; then
    echo "Respuesta inesperada: $valor" >&2
    exit 1
  fi
  echo "$valor"
}

n="$RANDOM"
clave="Aa1#$(date +%s)xyz"
fecha_cita="2035-06-15T10:00:00"
hoy="$(date -u +%F)"

echo "== Autenticacion"
ADMIN="$(token_de admin "$ADMIN_PASSWORD")"
if [ -z "$ADMIN" ] || [ "${#ADMIN}" -lt 40 ]; then
  echo "No se pudo iniciar sesion como admin" >&2
  exit 1
fi
printf 'POST    /login (credenciales invalidas)   -> '
llamar -o /dev/null -w '%{http_code}\n' -X POST "$BASE/login" -H "Content-Type: application/json" \
  -d '{"username":"usuario-inexistente","password":"incorrecta"}'

echo "== Usuarios por rol"
enviar POST /usuarios "$ADMIN" "{\"username\":\"recep$n\",\"email\":\"recep$n@vet.cl\",\"password\":\"$clave\",\"rol\":\"RECEPCIONISTA\"}" > /dev/null
enviar POST /usuarios "$ADMIN" "{\"username\":\"vet$n\",\"email\":\"vet$n@vet.cl\",\"password\":\"$clave\",\"rol\":\"VETERINARIO\"}" > /dev/null
RECEP="$(token_de "recep$n" "$clave")"
VET="$(token_de "vet$n" "$clave")"
estado GET /usuarios "$ADMIN"

echo "== Flujo completo como RECEPCIONISTA y ADMIN"
PROP="$(enviar POST /propietarios "$RECEP" "{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"rut\":\"$((10000000 + RANDOM))-5\",\"email\":\"ana$n@correo.cl\",\"telefono\":\"+56911112222\",\"direccion\":\"Calle Uno 123\"}" | identificador)"
MASC="$(enviar POST /mascotas "$RECEP" "{\"nombre\":\"Firulais\",\"especie\":\"Perro\",\"raza\":\"Labrador\",\"edad\":5,\"propietarioId\":$PROP}" | identificador)"
VETID="$(enviar POST /veterinarios "$ADMIN" "{\"nombre\":\"Carlos Soto\",\"especialidad\":\"Medicina general\",\"email\":\"carlos$n@vet.cl\"}" | identificador)"
CITA="$(enviar POST /citas "$RECEP" "{\"fechaHora\":\"$fecha_cita\",\"motivo\":\"Control anual\",\"mascotaId\":$MASC,\"veterinarioId\":$VETID}" | identificador)"
REG="$(enviar POST /registros-medicos "$VET" "{\"fecha\":\"$hoy\",\"diagnostico\":\"Sano\",\"tratamiento\":\"Vacuna anual\",\"mascotaId\":$MASC,\"veterinarioId\":$VETID,\"citaId\":$CITA}" | identificador)"
FAC="$(enviar POST /facturas "$RECEP" "{\"propietarioId\":$PROP,\"detalles\":[{\"descripcion\":\"Consulta\",\"cantidad\":1,\"precioUnitario\":25000.00},{\"descripcion\":\"Vacuna\",\"cantidad\":2,\"precioUnitario\":12500.50}]}" | identificador)"
echo "propietario=$PROP mascota=$MASC veterinario=$VETID cita=$CITA registro=$REG factura=$FAC"

estado GET /propietarios "$RECEP"
estado GET "/propietarios/$PROP" "$VET"
estado GET /mascotas "$VET"
estado GET "/mascotas?propietarioId=$PROP" "$RECEP"
estado GET "/mascotas/$MASC" "$ADMIN"
estado GET /veterinarios "$RECEP"
estado GET "/veterinarios/$VETID" "$VET"
estado GET /citas "$VET"
estado GET "/citas?fecha=2035-06-15" "$RECEP"
estado GET "/citas/$CITA" "$ADMIN"
estado GET /registros-medicos "$VET"
estado GET "/registros-medicos?mascotaId=$MASC" "$VET"
estado GET "/registros-medicos/$REG" "$ADMIN"
estado GET /facturas "$RECEP"
estado GET "/facturas/$FAC" "$ADMIN"

enviar PUT "/propietarios/$PROP" "$RECEP" "{\"nombre\":\"Ana Maria\",\"apellido\":\"Perez\",\"rut\":\"$((10000000 + RANDOM))-5\",\"email\":\"ana$n@correo.cl\",\"telefono\":\"+56911112222\",\"direccion\":\"Calle Dos 456\"}" > /dev/null
enviar PATCH "/citas/$CITA/estado" "$VET" '{"estado":"COMPLETADA"}' > /dev/null
enviar PATCH "/facturas/$FAC/pagar" "$RECEP" > /dev/null
estado GET "/facturas/$FAC" "$RECEP"

echo "== Controles de acceso (se esperan 401, 403 y 400)"
estado GET /citas
estado GET /citas "token.invalido.firma"
estado GET /registros-medicos "$RECEP"
estado POST /facturas "$VET"
estado DELETE "/mascotas/$MASC" "$RECEP"
estado GET /usuarios "$VET"
estado GET /ruta-inexistente "$ADMIN"
printf 'POST    /propietarios (cuerpo vacio)      -> '
llamar -o /dev/null -w '%{http_code}\n' -X POST "$BASE/propietarios" -H "Authorization: Bearer $ADMIN" -H "Content-Type: application/json" -d '{}'

echo "== Eliminaciones como ADMIN (se espera 409 en propietarios porque tiene factura)"
estado DELETE "/registros-medicos/$REG" "$ADMIN"
estado DELETE "/propietarios/$PROP" "$ADMIN"
estado DELETE "/citas/$CITA" "$ADMIN"
estado DELETE "/mascotas/$MASC" "$ADMIN"
estado DELETE "/veterinarios/$VETID" "$ADMIN"
echo "Recorrido terminado"
