# GestoPago Digital Commerce — Ecosistema Bancario y Pagos de Servicios

Plataforma integral de banca digital y pago de servicios compuesta por un **Backend REST en Spring Boot**, una **Aplicación Web en producción (Vercel)** y una **Aplicación Móvil nativa en Flutter**, conectadas en tiempo real con la pasarela de **GestoPago** (`sendTx.do`, `confirmTx.do`, `getProductList.do`).

---

## Descarga y Accesos en Producción

| Componente | Enlace / Ubicación | Descripción |
| :--- | :--- | :--- |
| **App Móvil Android (APK Release)** | [Descargar GestoPago APK (`gestopago-app-release.apk`)](./releases/gestopago-app-release.apk) \| [Descarga directa GitHub](https://github.com/juanpaa777/proyecto_H_JDPX/raw/main/releases/gestopago-app-release.apk) | Compilación release (`52.2 MB`) conectada al backend en producción. |
| **Aplicación Web (Vercel)** | https://gestopagoweb.vercel.app | Cliente web responsivo con proxy HTTPS hacia Render (`vercel.json`). |
| **Backend API (Render)** | https://gestopago-backend-ff40.onrender.com | API REST Spring Boot con Swagger (`/swagger-ui.html`) y Actuator (`/actuator/health`). |

---

## Índice de Documentación Técnica

| Documento | Contenido |
| :--- | :--- |
| [Arquitectura y Decisiones de Seguridad](./docs/ARQUITECTURA_Y_SEGURIDAD.md) | Clean Architecture, Cubit/BLoC, Sesión Única en PostgreSQL, 2FA TOTP (RFC 6238), Biometría por cuenta, Anti-IDOR y cifrado AES-256-CBC. |
| [Inventario de APIs y Endpoints REST](./docs/API_ENDPOINTS.md) | Especificación completa de endpoints (URLs, métodos HTTP, headers, payloads JSON de entrada/salida, códigos de estado y validaciones). |
| [Despliegue, Ambientes y Pruebas](./docs/DESPLIEGUE_Y_PRUEBAS.md) | Variables de entorno, PostgreSQL, MongoDB, Render/Vercel, Google Apps Script y ejecución de pruebas automatizadas. |
| [Script de Envío de Tickets HTML](./docs/google-apps-script-ticket.gs) | Google Apps Script Web App para envío automático de comprobantes HTML por correo electrónico. |

---

## Arquitectura General del Ecosistema

```
┌─────────────────────────┐       HTTPS / JSON       ┌──────────────────────────────┐
│   App Móvil (Flutter)   │ ───────────────────────► │                              │
│ Clean Arch + Cubit/Dio  │                          │   Backend API (Spring Boot)  │
│ • Huella por cuenta     │                          │  • JWT + Sesión Única (PG)   │
│ • 2FA TOTP / Re-link    │                          │  • TOTP 2FA (RFC 6238)       │
└─────────────────────────┘                          │  • Anti-IDOR + BCrypt        │
                                                     │  • Idempotencia (UPC)        │
┌─────────────────────────┐       HTTPS / JSON       │  • Cifrado AES-256-CBC       │
│    App Web (Vercel)     │ ───────────────────────► │  • Tickets Email (AppsScript)│
│  ES Modules + Tailwind  │                          └──────────────┬───────────────┘
└─────────────────────────┘                                         │
                                               ┌────────────────────┼────────────────────┐
                                               ▼                    ▼                    ▼
                                        PostgreSQL              MongoDB           GestoPago REST
                                  (Clientes, Cuentas,         (Catálogo y        (authenticate /
                                   Sesiones, 2FA, Pagos)       Logs XML)          getProductList /
                                                                                  sendTx / confirmTx)
```

---

## Funcionalidades Clave

### 1. Autenticación, Sesión Única y 2FA (Google Authenticator)
- **Sesión Única en Servidor:** Control estricto respaldado en PostgreSQL; si existe una sesión activa en otro dispositivo devuelve `409 Conflict` permitiendo cerrarla de forma controlada.
- **2FA TOTP (RFC 6238):** Vinculación mediante QR (`otpauth://`) y clave manual, ventana ±30s, límite de intentos y protección anti-reuso (`totpUltimoPaso`).
- **Re-vinculación en cambio de dispositivo:** Botón *"¿Cambiaste de dispositivo? Volver a vincular Authenticator"* disponible directamente en la pantalla de verificación 2FA (`POST /auth/totp-relink`).
- **Biometría aislada por cuenta (Móvil):**
  - Cada cuenta registrada en el teléfono tiene su propia configuración biométrica (`isBiometricEnrolledFor(correo)`), evitando que una cuenta nueva herede la huella de otra.
  - Al primer inicio de sesión se solicita activar y verificar la huella/rostro; al iniciar sesión con huella se omite el 2FA redundante.

### 2. Pagos Reales con GestoPago, Trazabilidad y Categoría `FALLIDOS`
- **Autorización sin contraseña de acceso:**
  - **Móvil:** Si la cuenta tiene biometría activada autoriza con **Huella / Rostro**; de lo contrario solicita el **código 2FA de 6 dígitos** de Google Authenticator.
  - **Web:** Autoriza mediante el **código 2FA de 6 dígitos**.
- **Idempotencia (UPC):** Generado por la aplicación de forma 100% alfanumérica sin guiones (ej. `GP834921AB3F9K`) para prevenir cobros duplicados.
- **Conexión real con GestoPago:**
  - Si GestoPago responde código `01`, se descuenta el saldo y se guarda como **`EXITOSA`** con su folio de autorización.
  - Si GestoPago rechaza la operación (por ejemplo código `05` / `NUM_AUTORIZACION = -1` por restricción de IP), **no se descuenta el saldo**, se conserva el número exacto devuelto por GestoPago (`-1`, `0` o `1`) y se registra en la categoría **`FALLIDOS`**.
- **Comprobantes completos:** El ticket digital (App, Web, Descargable/Imprimible y Correo) incluye estatus, folio exacto de autorización de GestoPago, UPC, referencia, saldo y la **leyenda oficial del servicio** (`leyenda` / `legend`).

### 3. Envío Automático de Tickets HTML por Correo
- Al finalizar cualquier operación (`EXITOSA` o `FALLIDOS`), el backend envía automáticamente el comprobante HTML formateado al correo del cliente titular mediante **Google Apps Script** (`GOOGLE_SCRIPT_TICKET_URL`), compatible con Render Free Tier sin puertos SMTP bloqueados.

---

## Variables de Entorno Principales (Backend)

| Variable | Uso |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` / `PGUSER` / `PGPASSWORD` | Conexión a PostgreSQL |
| `SPRING_DATA_MONGODB_URI` | Conexión a MongoDB (catálogo de productos) |
| `GESTOPAGO_ID_DISTRIBUIDOR` / `GESTOPAGO_CODIGO_DISPOSITIVO` / `GESTOPAGO_PASSWORD` | Credenciales JWT de GestoPago |
| `GESTOPAGO_API_KEY` / `GESTOPAGO_AES_SECRET_KEY` / `GESTOPAGO_AES_IV` | Firma y cifrado AES-256-CBC para `sendTx.do` y `confirmTx.do` |
| `GESTOPAGO_FALLBACK_ORIGIN` | `false` por defecto (respuestas reales de GestoPago sin simulación) |
| `GOOGLE_SCRIPT_TICKET_URL` | URL de la Web App de Google Apps Script para tickets por correo |

---

## Ejecución Local y Pruebas

- **Backend local:** `.\gradlew.bat bootRun`
- **Pruebas Backend:** `.\gradlew.bat test`
- **Pruebas Flutter:** `flutter test`
- **Compilar APK Release:** `flutter build apk --release`
- **Despliegue Web (Vercel):** `vercel --prod --yes`
