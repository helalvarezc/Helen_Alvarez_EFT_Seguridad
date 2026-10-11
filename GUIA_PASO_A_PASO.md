# Guía paso a paso – EFT Seguridad y Calidad en el Desarrollo (CDY2203)

Proyecto: backend de gestión veterinaria (Spring Boot 3.5, Java 17, Spring Security + JWT, MySQL, Apache HTTPS).

> **Importante sobre lo que se verificó.** El código se revisó de forma estática (sintaxis con `javac`, que todos los `import` y paquetes existan, que cada método usado por servicios y pruebas esté definido, y que ningún archivo del proyecto tenga comentarios), pero **no se pudo compilar ni ejecutar en el entorno donde fue creado** porque no había acceso a Maven Central ni a Docker. Por eso el primer paso de esta guía es ejecutar `mvn -B clean verify` en tu computador. Si algo falla, copia el mensaje de error completo y se corrige.

Todos los comandos se ejecutan desde la carpeta raíz del proyecto (donde está `pom.xml`) salvo que se indique otra ruta. Las instrucciones usan **Git Bash** (viene con Git para Windows). Donde cambia en PowerShell se indica.

---

## 0. Orden recomendado

| Orden | Paso EFT | Qué haces | Tiempo aprox. |
|---|---|---|---|
| 1 | Preparación | Requisitos, `.env`, certificado | 20 min |
| 2 | Paso 6 | Pruebas unitarias + JaCoCo | 10 min |
| 3 | Paso 1 | Levantar la aplicación con Docker y probarla | 20 min |
| 4 | GitHub | Subir el código | 10 min |
| 5 | Paso 5 | SCA con OWASP Dependency-Check | 15 a 30 min |
| 6 | Paso 4 | OpenVAS (**déjalo sincronizando primero, tarda mucho**) | 1 a 2 h (en segundo plano) |
| 7 | Paso 3 | SonarQube + Jenkins | 40 min |
| 8 | Paso 2 | ZAP | 40 min |
| 9 | Paso 7 y 8 | Informe PDF y video | 3 a 4 h |

Consejo: ejecuta el punto 6 (sincronización de OpenVAS) en cuanto termines el punto 3, y avanza con lo demás mientras tanto.

---

## 1. Preparación

### 1.1 Requisitos instalados

| Programa | Versión | Comprobar con |
|---|---|---|
| JDK | 17 o superior | `java -version` |
| Maven | 3.9 o superior | `mvn -version` |
| Docker Desktop | reciente (con 6 GB de RAM asignados) | `docker --version` y `docker compose version` |
| Git (incluye Git Bash y OpenSSL) | reciente | `git --version` y `openssl version` |
| OWASP ZAP | 2.15 o superior | abrir la aplicación |
| Insomnia o Postman | cualquiera | abrir la aplicación |

### 1.2 Estructura del proyecto

```
veterinaria-backend/
  pom.xml
  Dockerfile
  docker-compose.yml
  .env.example
  sonar-project.properties
  database/schema.sql            esquema de la base de datos (entregable)
  database/data.sql              datos de ejemplo
  src/main/java/com/duoc/backend/
    config/  controller/  dto/  exception/  model/  repository/  security/  service/
  src/test/java/com/duoc/backend/ pruebas JUnit, Mockito y MockMvc (entregable)
  apache/                        proxy HTTPS (Dockerfile y httpd.conf)
  ci/                            Jenkins + SonarQube
  openvas/                       OpenVAS (Greenbone)
  scripts/recorrer-api.sh        recorre todos los endpoints (sirve para ZAP)
```

### 1.3 Archivo `.env` (en la raíz del proyecto)

Crea el archivo copiando la plantilla:

```
cp .env.example .env
```

Abre `.env` con VS Code y completa **todos** los valores. Usa solo letras, números, guion `-` y guion bajo `_` (evita `$`, `#`, comillas y espacios):

```
DB_ROOT_PASSWORD=RootVet_2026_x9K2
DB_NAME=veterinaria
DB_USER=vetuser
DB_PASSWORD=UserVet_2026_p4Q7
JWT_SECRET=PEGA_AQUI_EL_VALOR_GENERADO
ADMIN_PASSWORD=Admin-Vet_2026_m8T3
CORS_ORIGINS=https://localhost
```

Cambia las claves de ejemplo por las tuyas. Para generar `JWT_SECRET` (debe ser Base64 de al menos 32 bytes):

Git Bash:

```
openssl rand -base64 48
```

PowerShell:

```
$b = New-Object byte[] 48; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Copia el resultado en `JWT_SECRET=` (sin comillas). El usuario `admin` se crea automáticamente al iniciar la aplicación con la clave de `ADMIN_PASSWORD`.

### 1.4 Certificado HTTPS (carpeta `apache/certs`)

En Git Bash, desde la raíz:

```
MSYS_NO_PATHCONV=1 openssl req -x509 -nodes -newkey rsa:2048 -days 365 -keyout apache/certs/server.key -out apache/certs/server.crt -subj "/CN=localhost" -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

Comprueba que existan los dos archivos:

```
ls apache/certs
```

Debe mostrar `server.crt` y `server.key`. Estos archivos **no** se suben a GitHub (están en `.gitignore`).

---

## 2. Paso 6 – Pruebas unitarias y cobertura (JaCoCo)

No necesita Docker: las pruebas usan una base H2 en memoria que se crea con `database/schema.sql`.

```
mvn -B clean verify
```

Qué debes ver al final:

```
Tests run: NN, Failures: 0, Errors: 0, Skipped: 0
...
BUILD SUCCESS
```

Si la cobertura de líneas bajara de 60 %, la compilación fallaría en la fase `verify` (regla configurada en `pom.xml`).

Abre el reporte de cobertura:

- Windows: `start target/site/jacoco/index.html`
- Linux: `xdg-open target/site/jacoco/index.html`

**Evidencias para el informe:**

1. Captura de la terminal con `Tests run: ... Failures: 0, Errors: 0` y `BUILD SUCCESS`.
2. Captura de `index.html` de JaCoCo con la fila **Total** (porcentaje de instrucciones y de ramas).
3. Captura de la tabla por paquete (`controller`, `service`, `security`, `exception`, `dto`, `model`, `config`). La guía de la semana 8 pide el porcentaje total, el porcentaje por paquete y cuáles tienen menor cobertura; destaca esos paquetes.
4. Guarda la carpeta `target/site/jacoco` y `target/surefire-reports` como respaldo.

Qué cubren las pruebas (para describirlo en el informe):

| Archivo de prueba | Qué valida |
|---|---|
| `controller/*ControllerTest` | CRUD de cada entidad con MockMvc: 200, 201, 204, 400, 404, 409 |
| `controller/AuthIntegrationTest` | Login correcto, credenciales inválidas, token vencido o mal firmado, bloqueo tras 5 intentos |
| `controller/RolesIntegrationTest` | Matriz de permisos por rol (403 y 401) |
| `service/CitaServiceTest`, `FacturaServiceTest`, `UsuarioServiceTest`, `AdminInicializadorTest` | Reglas de negocio con Mockito |
| `security/JwtServiceTest`, `AuthServiceTest`, `LoginAttemptServiceTest`, `CustomUserDetailsServiceTest` | Seguridad: tokens, bloqueo y usuarios |

---

## 3. Paso 1 – Levantar la aplicación (backend + MySQL + Apache HTTPS)

### 3.1 Construir y ejecutar

```
docker compose up -d --build
```

La primera vez tarda varios minutos (descarga imágenes y compila el JAR dentro de Docker). Comprueba:

```
docker compose ps
docker compose logs backend --tail 40
```

Los tres contenedores (`vet-db`, `vet-backend`, `vet-proxy`) deben estar `running`, y en el log del backend debe aparecer `Started BackendApplication` y `Usuario administrador inicial creado`.

Apagar: `docker compose down`. Borrar también los datos: `docker compose down -v` (necesario si cambias `schema.sql`, porque MySQL solo ejecuta los scripts la primera vez).

### 3.2 Probar con Insomnia

1. Preferences → Request / Response → desactiva **Validate certificates** (el certificado es autofirmado).
2. Nueva petición `POST https://localhost/login`, Body → JSON:

```
{"username":"admin","password":"EL_VALOR_DE_ADMIN_PASSWORD"}
```

3. Respuesta esperada `200`:

```
{"token":"eyJ...","tipo":"Bearer","expiraEnMs":3600000}
```

4. Petición `GET https://localhost/propietarios`, pestaña Auth → Bearer Token → pega el token. Debe responder `200` con los dos propietarios de ejemplo.

También puedes probar todo de una vez con el script (Git Bash):

```
export ADMIN_PASSWORD='EL_VALOR_DE_ADMIN_PASSWORD'
bash scripts/recorrer-api.sh
```

Códigos esperados: casi todo `200`/`201`; en la sección de controles de acceso `401` (sin token y token inválido), `403` (permisos insuficientes y ruta inexistente), `400` (cuerpo vacío); al eliminar el propietario `409` (tiene factura asociada).

### 3.3 Endpoints y permisos por rol

| Recurso | ADMIN | RECEPCIONISTA | VETERINARIO |
|---|---|---|---|
| `POST /login` | público | público | público |
| `/usuarios` (GET, POST) | sí | no | no |
| `/propietarios`, `/mascotas` GET | sí | sí | sí |
| `/propietarios`, `/mascotas` POST, PUT | sí | sí | no |
| `/veterinarios` GET | sí | sí | sí |
| `/veterinarios` POST, PUT | sí | no | no |
| `/citas` GET | sí | sí | sí |
| `/citas` POST, PUT | sí | sí | no |
| `PATCH /citas/{id}/estado` | sí | sí | sí |
| `/registros-medicos` (todo salvo DELETE) | sí | no | sí |
| `/facturas`, `PATCH /facturas/{id}/pagar` y `/anular` | sí | sí | no |
| Cualquier `DELETE` | sí | no | no |

### 3.4 Medidas de seguridad implementadas (para el informe)

| OWASP Top 10 (2021) | Cómo se mitiga | Dónde está |
|---|---|---|
| A01 Control de acceso | Roles por ruta y método, `denyAll` por defecto | `config/SecurityConfig.java` |
| A02 Fallas criptográficas | Contraseñas con BCrypt (costo 12), JWT firmado con HMAC-SHA (HS384 con el secreto de 48 bytes) y secreto externo, TLS 1.2 y 1.3 | `config/AppConfig.java`, `security/JwtService.java`, `apache/httpd.conf` |
| A03 Inyección | JPA con consultas parametrizadas, validación con Bean Validation, propiedades desconocidas rechazadas | `dto/*Request.java`, `application.properties` |
| A04 Diseño inseguro | Arquitectura por capas, DTO (no se exponen entidades), reglas de negocio (horario ocupado, estados) | `service/*` |
| A05 Configuración incorrecta | Cabeceras de seguridad, errores sin detalle técnico, CORS restringido, `ServerTokens Prod`, `TraceEnable Off`, contenedor sin root | `SecurityConfig`, `httpd.conf`, `Dockerfile` |
| A06 Componentes vulnerables | SCA con Dependency-Check y versiones gestionadas por Spring Boot | `pom.xml` |
| A07 Fallas de autenticación | Bloqueo de 15 minutos tras 5 fallos, token de 1 hora, mensaje genérico | `security/LoginAttemptService.java`, `AuthService.java` |
| A08 Integridad | Firma y emisor del JWT verificados en cada petición, imagen construida desde el código fuente | `security/JwtService.java`, `Dockerfile` |
| A09 Registro y monitoreo | Logs de login exitoso, fallido, cuenta bloqueada y acceso denegado, sin datos sensibles | `security/*` |
| A10 SSRF | La aplicación no realiza llamadas a URL entregadas por el usuario | no aplica |

---

## 4. Subir el código a GitHub

1. Crea un repositorio vacío en GitHub llamado `veterinaria-backend` (sin README).
2. En la raíz del proyecto:

```
git init
git add .
git commit -m "Backend veterinaria"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/veterinaria-backend.git
git push -u origin main
```

3. Comprueba que **no** se subieron secretos (no debe imprimir nada):

```
git ls-files | grep -E "(^|/)\.env$|server\.key|server\.crt"
```

Deja el repositorio **público** mientras Jenkins lo clona y mientras se corrige la evaluación, o agrega un token de acceso personal como credencial en Jenkins.

---

## 5. Paso 5 – SCA con OWASP Dependency-Check

1. Solicita la API key gratuita del NVD en `https://nvd.nist.gov/developers/request-an-api-key` (llega por correo). Sin ella la primera descarga puede fallar o tardar horas.
2. Define la clave en la terminal donde ejecutarás el análisis:

Git Bash:

```
export NVD_API_KEY=TU_CLAVE
```

PowerShell:

```
$env:NVD_API_KEY="TU_CLAVE"
```

3. Ejecuta (la primera vez demora unos 10 minutos o más). La clave se pasa de forma explícita:

Git Bash:

```
mvn -B dependency-check:check -DnvdApiKey="$NVD_API_KEY"
```

PowerShell:

```
mvn -B dependency-check:check "-DnvdApiKey=$env:NVD_API_KEY"
```

El plugin ya está configurado en `pom.xml` con `failBuildOnCVSS=9`: si existe una vulnerabilidad crítica (CVSS 9 o más) el comando termina con `BUILD FAILURE`.

4. Abre el reporte:

- `target/dependency-check-report.html` (para el informe)
- `target/dependency-check-report.json`

5. En el HTML revisa la tabla **Summary of Dependencies**, columna **Highest Severity**. Meta: ninguna dependencia con severidad **Critical**.
6. **Si aparece alguna crítica**: abre el detalle (CVE), mira la versión que corrige y actualiza `pom.xml`. En la mayoría de los casos basta subir la versión de Spring Boot a la última 3.5.x (línea `<version>3.5.6</version>` dentro de `<parent>`). Luego:

```
mvn -B clean verify
mvn -B dependency-check:check -DnvdApiKey="$NVD_API_KEY"
```

7. Un hallazgo que sea falso positivo se documenta en el informe (nombre de la librería, CVE y motivo). No se ignora sin explicación.

**Evidencias:** captura del comando terminando en `BUILD SUCCESS`, captura de la tabla resumen del HTML, y el HTML guardado. Si hubo críticas, guarda también el reporte inicial para mostrar el antes y el después.

---

## 6. Paso 4 – Detección de vulnerabilidades con OpenVAS

### 6.1 Levantar OpenVAS y esperar la sincronización

La aplicación debe estar corriendo (punto 3). Luego:

```
cd openvas
docker compose up -d
```

Abre `http://localhost:9392` e ingresa con usuario `admin` y contraseña `admin` (según la guía de la semana 5; cámbiala si el sistema lo solicita). Ve a **Administration → Feed Status** y espera a que todos los feeds digan **Current**. Puede tardar de 30 minutos a 2 horas la primera vez. No inicies el escaneo antes.

### 6.2 Conectar OpenVAS con la red de la aplicación

Desde cualquier carpeta:

```
docker network ls
```

Busca el nombre de la red de la aplicación, normalmente `veterinaria-backend_vet_net` (la carpeta del proyecto + `_vet_net`). Conecta los contenedores de escaneo a esa red:

```
docker network connect veterinaria-backend_vet_net greenbone-community-edition-openvasd-1
docker network connect veterinaria-backend_vet_net greenbone-community-edition-ospd-openvas-1
```

Si tu carpeta se llama distinto, usa el nombre que mostró `docker network ls`. Obtén las IP de los contenedores a analizar:

```
docker inspect -f "{{.Name}} {{range .NetworkSettings.Networks}}{{.IPAddress}} {{end}}" vet-proxy vet-backend
```

### 6.3 Ejecutar el escaneo

1. En `http://localhost:9392`: **Scans → Tasks**, icono de varita (Task Wizard).
2. En el campo de IP escribe la IP de `vet-proxy` y pulsa **Start Scan**. Repite con la IP de `vet-backend`.
3. Cuando el estado sea **Done**, abre el reporte (clic en la fecha en la columna Last Report). En la pestaña **Results** ordena por **Severity**.
4. Descarga el reporte: en el reporte usa el selector de formato (PDF o HTML) y el icono de descarga. Guarda `openvas-inicial-proxy.pdf` y `openvas-inicial-backend.pdf`.

### 6.4 Corregir y volver a escanear

Meta: **ningún hallazgo crítico** (severidad 9.0 o más; en Greenbone también revisa los de severidad High 7.0 a 8.9 y corrige los que puedas).

| Hallazgo típico | Acción |
|---|---|
| Versión de Apache con CVE | `docker compose build --no-cache proxy` y `docker compose up -d` (descarga la última imagen `httpd:2.4-alpine`) |
| Versión de Java con CVE | `docker compose build --no-cache --pull backend` y `docker compose up -d` |
| MySQL con CVE | `docker compose pull db` y `docker compose up -d` |
| TLS: certificado autofirmado | Es esperado en un entorno de pruebas; se documenta como riesgo aceptado y en producción se usa un certificado de una CA |
| `TCP timestamps`, `ICMP timestamp` (severidad baja) | No son críticos; se documentan |
| Puerto 3306 expuesto | Ya está limitado a `127.0.0.1` en `docker-compose.yml` |

Vuelve a escanear las mismas IP (las IP pueden cambiar al recrear contenedores; verifícalas otra vez) y descarga `openvas-final-proxy.pdf` y `openvas-final-backend.pdf`.

**Evidencias:** capturas del Feed Status, del Task Wizard, de la lista de resultados y los reportes descargados (antes y después).

---

## 7. Paso 3 – Análisis estático con SonarQube y Jenkins

### 7.1 Levantar Jenkins y SonarQube

```
cd ci
docker compose up -d --build
```

La primera construcción instala Maven dentro de la imagen de Jenkins. Si usas Linux, antes ejecuta una vez `sudo sysctl -w vm.max_map_count=262144` (SonarQube lo exige).

### 7.2 SonarQube (`http://localhost:9000`)

1. Usuario `admin`, contraseña `admin`; te pedirá cambiarla.
2. Arriba a la derecha: icono de usuario → **My Account → Security → Generate Tokens**. Nombre `jenkins`, tipo **Global Analysis Token** (o User Token). Copia el token (solo se muestra una vez).

### 7.3 Jenkins (`http://localhost:8080/jenkins`)

1. Obtén la clave inicial:

```
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

2. Pégala, elige **Install suggested plugins** y crea tu usuario administrador.
3. **Manage Jenkins → Plugins → Available plugins**: busca **SonarQube Scanner**, instálalo y reinicia Jenkins.
4. **Manage Jenkins → System → SonarQube servers → Add SonarQube**:
   - Name: `sonarqube`
   - Server URL: `http://sonarqube:9000`
   - Server authentication token → Add → Jenkins: Kind **Secret text**, Secret = el token del paso 7.2, ID `sonar-token` → Add → selecciónalo.
   - Save.
5. **Manage Jenkins → Tools → SonarQube Scanner installations → Add**: Name `sonar-scanner`, marca **Install automatically**. Save.

### 7.4 Crear el job

1. **New Item** → nombre `veterinaria-sonar` → **Freestyle project** → OK.
2. **Source Code Management → Git**: Repository URL = la URL de tu repositorio de GitHub; Branch Specifier `*/main`.
3. **Build Steps → Add build step → Execute shell**:

```
mvn -B clean verify
```

4. **Add build step → Execute SonarQube Scanner**, y en **Analysis properties** pega:

```
sonar.projectKey=veterinaria
sonar.projectName=veterinaria
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=target/classes
sonar.java.test.binaries=target/test-classes
sonar.sourceEncoding=UTF-8
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```

5. Save → **Build Now**. Abre el build → **Console Output**; debe terminar con `Finished: SUCCESS`.
6. Si el paso de Sonar dice que no encuentra la URL del servidor, en la configuración del job marca **Build Environment → Prepare SonarQube Scanner environment** y vuelve a ejecutar.

### 7.5 Revisar y corregir

1. En `http://localhost:9000` abre el proyecto `veterinaria`. Revisa **Quality Gate**, **Security**, **Reliability** y **Maintainability**.
2. **Issues**: filtra por severidad (**Blocker** y **Critical**; en versiones nuevas aparece como impacto **Blocker** y **High**). Meta: **0**.
3. **Security Hotspots**: revisa cada uno y márcalo **Safe** o **Fixed** con comentario. Caso esperado: *CSRF desactivado* en `SecurityConfig.java`; comentario sugerido: "API REST sin estado que autentica con JWT en la cabecera Authorization; no usa cookies ni sesiones, por lo tanto no es vulnerable a CSRF".
4. Si corriges código: guarda, `git add .`, `git commit -m "Correcciones Sonar"`, `git push` y vuelve a ejecutar **Build Now**.

**Evidencias:** captura del job con build en verde, del Overview de SonarQube con Quality Gate **Passed**, de Issues filtrado por Blocker/Critical mostrando 0, de los Security Hotspots revisados y de la cobertura que muestra Sonar. Guarda la primera ejecución si tenía hallazgos para mostrar el antes y el después.

---

## 8. Paso 2 – OWASP ZAP

### 8.1 Preparar ZAP

1. Abre ZAP. **Tools → Options → Network → Local Servers/Proxies**: Address `localhost`, Port `8090` (el 8080 lo usa Jenkins). OK.
2. Si ZAP rechaza el certificado autofirmado, en **Tools → Options → Network → Server Certificates** desactiva la validación de certificados solo para esta prueba local (el nombre de la opción varía según la versión).

### 8.2 Hacer que ZAP vea todos los endpoints

La aplicación es una API JSON sin enlaces HTML, así que el rastreo automático no la descubre. Se resuelve pasando el tráfico de todos los endpoints por ZAP.

1. Verifica primero el script sin ZAP (punto 3.2).
2. Ejecútalo pasando por ZAP (Git Bash):

```
export ADMIN_PASSWORD='EL_VALOR_DE_ADMIN_PASSWORD'
export ZAP_PROXY=http://localhost:8090
bash scripts/recorrer-api.sh
```

3. En ZAP, en el panel **Sites** debe aparecer `https://localhost` con las rutas `/login`, `/propietarios`, `/mascotas`, `/citas`, `/registros-medicos`, `/facturas`, `/veterinarios`, `/usuarios`.

### 8.3 Escaneo activo autenticado

1. Obtén un token de `admin` (Insomnia, punto 3.2). Dura 1 hora.
2. En ZAP: **Tools → Options → Replacer → Add**:
   - Description: `jwt-admin`
   - Match Type: **Request Header (will add if not present)**
   - Match String: `Authorization`
   - Replacement String: `Bearer ` seguido del token (con el espacio)
   - Enable marcado, Initiators: all
3. Clic derecho en `https://localhost` → **Include in Context → Default Context**.
4. Clic derecho en `https://localhost` → **Attack → Active Scan**, contexto `Default Context`, **Start Scan**. Espera a que llegue a 100 %.
5. Revisa la pestaña **Alerts**. Cuenta las alertas por severidad (High, Medium, Low, Informational).
6. **Report → Generate Report**: plantilla **Traditional HTML Report** (o la que prefieras), nombre `zap-inicial.html`. Captura el árbol de alertas.

### 8.4 Corregir y volver a escanear

El proyecto ya incluye cabeceras de seguridad en `apache/httpd.conf` y en `SecurityConfig.java`. Para cada alerta que ZAP aún reporte, aplica la corrección y reconstruye:

| Alerta de ZAP | Corrección (dentro de `<VirtualHost *:443>` en `apache/httpd.conf`) |
|---|---|
| Content Security Policy (CSP) Header Not Set | `Header always set Content-Security-Policy "default-src 'none'; frame-ancestors 'none'"` |
| Missing Anti-clickjacking Header | `Header always set X-Frame-Options "DENY"` |
| X-Content-Type-Options Header Missing | `Header always set X-Content-Type-Options "nosniff"` |
| Strict-Transport-Security Header Not Set | `Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains"` |
| Re-examine Cache-control Directives / Storable and Cacheable Content | `Header always set Cache-Control "no-store"` |
| Permissions Policy Header Not Set | `Header always set Permissions-Policy "geolocation=(), microphone=(), camera=()"` |
| Insufficient Site Isolation Against Spectre | `Header always set Cross-Origin-Resource-Policy "same-origin"` y `Cross-Origin-Opener-Policy "same-origin"` |
| Server Leaks Version Information | Ya está `ServerTokens Prod` y `ServerSignature Off` (queda solo "Apache", severidad baja) |
| Error Message / Application Error Disclosure | Ya está `server.error.include-*=never` y el manejador global de excepciones |

Después de modificar `httpd.conf`:

```
docker compose up -d --build proxy
```

Si corriges código Java: `docker compose up -d --build backend`.

Genera un token nuevo, actualiza la regla Replacer, vuelve a ejecutar el script y el escaneo activo, y exporta `zap-final.html`.

**Meta:** 0 alertas **High** y 0 **Medium**. Las **Low** o **Informational** que queden se listan en el informe con su justificación.

**Evidencias:** `zap-inicial.html` y `zap-final.html`, capturas de la pestaña Alerts antes y después, y una tabla alerta → categoría OWASP → corrección → estado.

---

## 9. Paso 7 – Informe PDF (10 a 15 páginas)

Usa el archivo `CDY2203_EFT_S9_Plantilla_informe_EFT.docx` (portada, índice, títulos Arial 22 negrita, subtítulos Arial 16 negrita, cuerpo Arial 12 justificado). Estructura sugerida y páginas:

| Sección | Contenido | Págs. |
|---|---|---|
| Portada e índice | Nombre, sección, docente, fecha | 1 |
| 1. Introducción y caso | Veterinaria, objetivo, alcance | 1 |
| 2. Arquitectura | Capas, tecnologías, modelo de datos (usa `schema.sql`), roles | 1 a 2 |
| 3. Seguridad implementada | Tabla OWASP del punto 3.4 | 1 |
| 4. ZAP | Proceso, alertas iniciales, correcciones, reporte final | 2 |
| 5. Análisis estático | Jenkins + SonarQube, hallazgos, correcciones, Quality Gate | 2 |
| 6. OpenVAS | Proceso, hallazgos, correcciones, resultado final | 2 |
| 7. SCA | Dependency-Check, resultado | 1 |
| 8. Pruebas y cobertura | Resultado de `mvn verify`, JaCoCo total y por paquete | 1 a 2 |
| 9. Conclusiones | Aprendizajes de la asignatura | 1 |
| Anexo | Enlace al repositorio y versiones de herramientas | media |

Cada captura lleva pie de figura ("Figura 3. Reporte final de ZAP sin alertas High ni Medium") y debe leerse con claridad. Exporta a PDF y verifica que no supere 15 páginas.

---

## 10. Paso 8 – Video (máximo 5 minutos)

| Tiempo | Qué mostrar |
|---|---|
| 0:00 a 0:30 | Presentación y caso de la veterinaria |
| 0:30 a 1:30 | Aplicación en ejecución: login, crear cita, registro médico, factura, y un acceso denegado (403) |
| 1:30 a 3:30 | Reportes finales: ZAP, SonarQube (Quality Gate), OpenVAS y Dependency-Check, todos sin críticos |
| 3:30 a 4:15 | `mvn clean verify` en verde y reporte JaCoCo |
| 4:15 a 5:00 | Aprendizajes y cierre |

Grábalo con Kaltura (instructivo de la actividad) y ensáyalo antes para no pasar de 5:00.

---

## 11. Entrega

1. Crea el zip del código con los archivos versionados (no incluye `.env`, certificados ni `target`):

```
git archive --format=zip --output=../codigo-veterinaria.zip HEAD
```

2. Junta el código y el informe (PowerShell, en la carpeta donde están ambos archivos):

```
Compress-Archive -Path codigo-veterinaria.zip, Informe_EFT.pdf -DestinationPath EFT_CDY2203_TU_NOMBRE.zip
```

3. En el AVA: sube el `.zip`, pega el **enlace del repositorio** en la sección habilitada y adjunta el **video**.
4. Antes de entregar, prueba que el repositorio funciona desde cero:

```
git clone https://github.com/TU_USUARIO/veterinaria-backend.git prueba-clon
cd prueba-clon
mvn -B clean verify
```

---

## 12. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `mvn verify` falla con `Table ... already exists` o no encuentra `database/schema.sql` | Ejecuta Maven desde la raíz del proyecto (donde está la carpeta `database`) |
| El contenedor `vet-proxy` se reinicia | Faltan `apache/certs/server.crt` o `server.key` (punto 1.4) |
| `vet-backend` termina con `Could not resolve placeholder 'JWT_SECRET'` | `.env` incompleto o `JWT_SECRET` vacío |
| `vet-backend`: `Illegal base64 character` o `key is too short` | `JWT_SECRET` no es Base64 válido de al menos 32 bytes; genera uno nuevo (punto 1.3) |
| Login responde 401 con la clave correcta | El usuario `admin` solo se crea si `ADMIN_PASSWORD` tenía valor la primera vez; si ya existía con otra clave, haz `docker compose down -v` y levanta de nuevo |
| Login responde 429 | Bloqueo de 15 minutos tras 5 fallos; espera o reinicia el backend |
| `database/schema.sql` no se aplica | Solo corre al crear el volumen de MySQL; usa `docker compose down -v` |
| Dependency-Check muy lento o con error 403/404 del NVD | Falta la clave: usa `-DnvdApiKey=...` como en el punto 5 |
| Jenkins: `mvn: command not found` | Construye la imagen desde `ci`: `docker compose up -d --build` |
| Sonar no muestra cobertura | Verifica que `mvn clean verify` generó `target/site/jacoco/jacoco.xml` y que la propiedad `sonar.coverage.jacoco.xmlReportPaths` está en el job |
| OpenVAS no encuentra el objetivo | Conecta los contenedores a la red de la aplicación (6.2) y usa las IP actuales |
| Puerto 80, 443, 8080 o 9000 en uso | Cierra el programa que lo usa o cambia el mapeo de puertos en el compose correspondiente |
