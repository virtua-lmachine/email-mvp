# Email MVP

Aplicación educativa y ligera para **enviar un correo desde una interfaz web**. No
usa base de datos, no guarda historial y no tiene usuarios. Su única finalidad es
mostrar cómo se comunican un frontend, una API REST, un servicio serverless y un
proveedor externo de correo.

```
[ Vue.js ] --HTTP/JSON--> [ Spring Boot ] --HTTPS--> [ Cloudflare Worker ] --API--> [ SendGrid ] --> Destinatario
```

La respuesta recorre el camino inverso y el frontend muestra si **SendGrid aceptó
la solicitud** o si ocurrió un error.

---

## Estructura

```
email-mvp/
├── frontend/   Vue 3 + Vite  (formulario y `fetch`)
├── backend/    Spring Boot    (API REST, validación, cliente HTTP)
└── worker/     Cloudflare Worker (llama a SendGrid)
```

Cada componente se ejecuta y despliega por separado.

---

## Requisitos

| Herramienta | Versión usada | Notas |
| --- | --- | --- |
| Node.js | 24.x | frontend y worker |
| Java (JDK) | 17+ | backend (Spring Boot 4) |
| Maven | — | se usa el wrapper `./mvnw`, no hace falta instalarlo |
| Wrangler | 4.x | incluido en el `package.json` del worker (`npx wrangler`) |
| Cuenta SendGrid | — | plan gratuito sirve para pruebas |
| Cuenta Cloudflare | — | plan gratuito sirve para desplegar el Worker |

---

## 1. Worker + SendGrid (empezar por aquí)

### 1.1 Cuenta y API Key de SendGrid

1. Crea una cuenta en https://signup.sendgrid.com y verifica tu correo.
2. Verifica un remitente en **Settings → Sender Authentication → Single Sender
   Verification** (o autentica tu dominio si tienes uno). Copia la dirección
   verificada: la usarás como `FROM_EMAIL`.
3. Crea una API Key en **Settings → API Keys → Create API Key** con permiso
   **Mail Send: Full Access**. Cópiala (solo se muestra una vez).

> Edita `worker/wrangler.toml` y cambia `FROM_EMAIL` por tu remitente verificado.

### 1.2 Ejecutar el Worker en local

```bash
cd worker
npm install

# Crea el archivo de secretos locales a partir del ejemplo
cp .dev.vars.example .dev.vars
# Edita .dev.vars y coloca tu SENDGRID_API_KEY y un WORKER_SHARED_SECRET largo
```

El archivo `worker/.dev.vars` **no** se sube al repositorio (está en `.gitignore`).

```bash
npx wrangler dev
```

El Worker queda disponible en `http://localhost:8787`.

### 1.3 Probar el Worker por separado

```bash
curl -i http://localhost:8787 \
  -H "content-type: application/json" \
  -H "x-shared-secret: TU_WORKER_SHARED_SECRET" \
  -d '{"to":"tu-correo@ejemplo.com","subject":"Prueba","message":"Hola desde el Worker"}'
```

- `202` en SendGrid → el Worker responde `{"ok":true,"message":"SendGrid aceptó la solicitud de envío"}`
- Si envías una petición sin el encabezado `x-shared-secret`, responde `401`.

### 1.4 Desplegar y guardar los secretos

```bash
cd worker
npx wrangler login

# Secretos de producción (te los pedirá de forma interactiva)
npx wrangler secret put SENDGRID_API_KEY
npx wrangler secret put WORKER_SHARED_SECRET

npx wrangler deploy
```

Wrangler imprime la URL pública, por ejemplo:
`https://email-mvp-worker.tu-subdominio.workers.dev`.

---

## 2. Backend (Spring Boot)

### 2.1 Ejecutar en local

```bash
cd backend

# Apunta al Worker local y usa el MISMO secreto compartido del Worker
export WORKER_URL=http://localhost:8787
export WORKER_SHARED_SECRET=el_mismo_secreto_del_paso_1.2

./mvnw spring-boot:run
```

El backend queda en `http://localhost:8080`.

### 2.2 Probar la API por separado

```bash
# Petición válida
curl -i http://localhost:8080/api/emails \
  -H "content-type: application/json" \
  -d '{"to":"tu-correo@ejemplo.com","subject":"Hola","message":"Correo de prueba"}'

# Petición inválida (debe responder 400 con el detalle de los campos)
curl -i http://localhost:8080/api/emails \
  -H "content-type: application/json" \
  -d '{"to":"no-es-un-correo","subject":"","message":""}'
```

### 2.3 Variables de configuración

Definidas en `backend/src/main/resources/application.properties`:

| Propiedad | Variable de entorno | Valor por defecto |
| --- | --- | --- |
| `app.worker.url` | `WORKER_URL` | `http://localhost:8787` |
| `app.worker.shared-secret` | `WORKER_SHARED_SECRET` | *(placeholder, cámbialo)* |
| `app.worker.connect-timeout-ms` | — | `5000` |
| `app.worker.read-timeout-ms` | — | `15000` |
| `app.cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |

### 2.4 Endpoint

`POST /api/emails`

```json
{ "to": "destinatario@ejemplo.com", "subject": "Hola", "message": "Correo de prueba" }
```

Respuesta correcta (`200`):

```json
{ "ok": true, "message": "SendGrid aceptó la solicitud de envío" }
```

Error de validación (`400`):

```json
{ "ok": false, "message": "Datos inválidos", "errors": { "to": "El destinatario no es un correo válido" } }
```

Error al contactar al Worker (`502`):

```json
{ "ok": false, "message": "No se pudo contactar al Worker (timeout o conexión rechazada)." }
```

---

## 3. Frontend (Vue 3 + Vite)

```bash
cd frontend
npm install

# Opcional: apuntar a otro backend
cp .env.example .env
# Edita VITE_API_URL si tu backend no está en http://localhost:8080

npm run dev
```

Abre `http://localhost:5173`. El formulario incluye validación de campos vacíos y
de formato de correo, indicador de carga, y mensajes de éxito o error.

Para generar la versión de producción:

```bash
npm run build      # genera dist/
npm run preview    # sirve dist/ en local
```

---

## 4. Flujo completo de prueba

1. Terminal 1 → `cd worker && npx wrangler dev`
2. Terminal 2 → `cd backend && WORKER_URL=http://localhost:8787 WORKER_SHARED_SECRET=... ./mvnw spring-boot:run`
3. Terminal 3 → `cd frontend && npm run dev`
4. Abre `http://localhost:5173`, completa el formulario y envía.

Prueba también: correo inválido, campos vacíos, backend apagado (error de
conexión) y secreto incorrecto (el Worker responde `401`).

---

## 5. Despliegue

Topología de producción:

```
[ Vue (hosting estático) ] --HTTPS--> [ Spring Boot en Render ] --HTTPS--> [ Worker en Cloudflare ] --> SendGrid
```

| Componente | Dónde | Notas |
| --- | --- | --- |
| Frontend | Hosting estático: **Render** (incluido en `render.yaml`) o Cloudflare Pages / Netlify | ejecuta `npm run build` y publica `dist/`. Define `VITE_API_URL` con la URL del backend. |
| Backend | **Render** (runtime Docker) | usa `backend/Dockerfile`. Variables: `WORKER_URL`, `WORKER_SHARED_SECRET`, `CORS_ALLOWED_ORIGINS`. |
| Worker | Cloudflare Workers | **no cambia**: `npx wrangler deploy` + secretos con `wrangler secret put`. |

### 5.1 Desplegar el Worker (sin cambios)

```bash
cd worker
npx wrangler login
npx wrangler secret put SENDGRID_API_KEY
npx wrangler secret put WORKER_SHARED_SECRET
npx wrangler deploy
```

Anota la URL resultante, p. ej. `https://email-mvp-worker.tu-subdominio.workers.dev`.
Opcionalmente ajusta `ALLOWED_ORIGIN` en `worker/wrangler.toml` a la URL de Render
(el Worker se llama servidor-a-servidor, así que CORS no se ejerce desde el navegador).

### 5.2 Desplegar el backend en Render

Render **no tiene runtime nativo de Java**, por eso el repo incluye
`backend/Dockerfile` (multi-stage: Maven compila el JAR y una imagen JRE lo ejecuta)
y un `render.yaml` de Blueprint. El backend escucha en `${PORT:8080}`, puerto que
Render inyecta automáticamente.

**Opción A — Blueprint (`render.yaml`):**

1. Sube el proyecto a un repositorio de GitHub/GitLab.
2. En Render: **New → Blueprint**, conecta el repo y selecciona el `render.yaml`
   de la raíz (`email-mvp/render.yaml`).
3. Cuando lo pida, escribe el valor de `WORKER_SHARED_SECRET` (el mismo del Worker).
4. Confirma `WORKER_URL` con la URL real del Worker y `CORS_ALLOWED_ORIGINS` con la
   URL del frontend.

**Opción B — Web Service manual:**

1. **New → Web Service**, conecta el repo.
2. **Language:** `Docker`.
3. **Root Directory:** `backend`.
4. **Dockerfile Path:** `./Dockerfile`.
5. **Instance Type:** Free (o superior).
6. En **Environment**, agrega:

   | Key | Value |
   | --- | --- |
   | `WORKER_URL` | `https://email-mvp-worker.tu-subdominio.workers.dev` |
   | `WORKER_SHARED_SECRET` | *(mismo secreto que el Worker)* |
   | `CORS_ALLOWED_ORIGINS` | `https://tu-frontend.example.com` (o `http://localhost:5173` en pruebas) |
   | `JAVA_OPTS` | `-Xmx384m` (ajuste opcional para el plan gratuito) |

7. Deploy. La primera compilación tarda más porque descarga dependencias Maven.

Prueba el backend desplegado:

```bash
curl -i https://tu-backend.onrender.com/api/emails \
  -H "content-type: application/json" \
  -d '{"to":"tu-correo@ejemplo.com","subject":"Hola","message":"Prueba desde Render"}'
```

### 5.3 Desplegar el frontend

El frontend es estático (Vite genera `dist/`) y el archivo `frontend/.nvmrc` fija
Node 22. Puedes alojarlo en **Render** (mismo `render.yaml`) o en **Cloudflare Pages**.

#### Opción 1 — Render (incluido en `render.yaml`)

El `render.yaml` ya define `email-mvp-frontend` como sitio estático
(`runtime: static`, `rootDir: frontend`). Al crear el Blueprint se despliega junto
con el backend, sin configuración extra.

- URL esperada: `https://email-mvp-frontend.onrender.com`
- Edita `VITE_API_URL` en `render.yaml` con la URL real del backend y vuelve a desplegar.

#### Opción 2 — Cloudflare Pages

**Desde el panel (recomendado):**

1. Sube el proyecto a GitHub/GitLab (mismo repo del backend).
2. Cloudflare Dashboard → **Workers & Pages → Create → Pages → Connect to Git**.
3. Selecciona el repositorio y configura:

   | Campo | Valor |
   | --- | --- |
   | Framework preset | `Vue` |
   | Build command | `npm run build` |
   | Build output directory | `dist` |
   | Root directory | `frontend` |

4. En **Environment variables (Production)** agrega:

   | Key | Value |
   | --- | --- |
   | `VITE_API_URL` | `https://tu-backend.onrender.com` |
   | `NODE_VERSION` | `22` *(opcional si detecta `.nvmrc`)* |

5. **Save and Deploy**. La URL será `https://<proyecto>.pages.dev`.

> `VITE_API_URL` se incrusta en el bundle **durante el build**. Si cambia, debes
> volver a desplegar (no basta con editar la variable).

**Con Wrangler (CLI):**

```bash
cd frontend
npm run build
npx wrangler pages deploy dist --project-name email-mvp-frontend
```

**Con Netlify:** Base directory `frontend`, Build command
`npm run build`, Publish directory `dist`, y variable `VITE_API_URL` con la URL de
Render.

### 5.4 Ajustes finales al desplegar

- `CORS_ALLOWED_ORIGINS` debe ser la URL real del frontend (varios separados por comas).
- `WORKER_URL` debe ser la URL HTTPS del Worker; `WORKER_SHARED_SECRET` debe coincidir
  exactamente con el secreto del Worker.
- En el frontend, `VITE_API_URL` debe apuntar a la URL HTTPS de Render.
- Usa siempre HTTPS entre todos los componentes.

> Nota del plan gratuito de Render: el servicio se suspende tras un periodo de
> inactividad y la primera petición tarda unos segundos en reactivarlo.

---

## 6. Seguridad (mínima, para un proyecto estudiantil)

- La **API Key de SendGrid** vive solo como secreto en Cloudflare, nunca en el código.
- Un **secreto compartido** (`WORKER_SHARED_SECRET`) protege el acceso al Worker.
- **CORS** limita qué origen puede llamar al backend (no es autenticación).
- **Validación** de entradas en Spring Boot y, de forma básica, en el Worker.
- **Límites** de tamaño de asunto (200) y mensaje (5000) para evitar abuso.
- No se registran claves, tokens ni el contenido del correo.

> Nunca incluyas el secreto del Worker ni la API Key en Vue.js: el código del
> frontend es público.

---

## 7. Alcance (lo que **no** incluye)

Sin base de datos, sin ORM, sin usuarios/roles, sin historial, sin panel de
administración, sin colas, sin microservicios, sin plantillas HTML avanzadas, sin
webhooks de entrega y sin Docker/Kubernetes.

La app solo confirma que **SendGrid aceptó la solicitud**; no garantiza la entrega
final salvo que incorpores un mecanismo de seguimiento (fuera del alcance del MVP).
