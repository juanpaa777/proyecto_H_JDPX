# GestoPago Digital Commerce — Ecosistema Bancario y Pagos de Servicios

Plataforma empresarial de comercio digital compuesta por un **Backend REST en Spring Boot**, una **Aplicación Web** y una **Aplicación Móvil en Flutter**, integradas en tiempo real con los servicios REST de **GestoPago**.

---

## Índice de Documentación

| Documento | Contenido |
| :--- | :--- |
| [Arquitectura y Decisiones de Seguridad](./docs/ARQUITECTURA_Y_SEGURIDAD.md) | Clean Architecture, BLoC/Cubit, Sesión Única en PostgreSQL, 2FA TOTP (RFC 6238), Anti-IDOR y manejo genérico de errores. |
| [Inventario de APIs y Endpoints REST](./docs/API_ENDPOINTS.md) | Especificación técnica de endpoints (URL, método HTTP, headers, JSON de entrada/salida, códigos de estado y validaciones). |
| [Despliegue, Ambientes y Pruebas](./docs/DESPLIEGUE_Y_PRUEBAS.md) | Configuración de ambientes (Local Docker vs Producción Render/Vercel/HTTPS), variables de entorno y suite de pruebas. |

---

## Arquitectura General del Ecosistema

```
┌─────────────────────────┐       HTTPS / JSON       ┌──────────────────────────────┐
│   App Móvil (Flutter)   │ ───────────────────────► │                              │
│  Clean Arch + Cubit/Dio │                          │   Backend API (Spring Boot)  │
└─────────────────────────┘                          │  • JWT + Sesión Única (PG)   │
                                                     │  • TOTP 2FA (RFC 6238)       │
┌─────────────────────────┐       HTTPS / JSON       │  • Anti-IDOR + BCrypt        │
│    App Web (Vercel)     │ ───────────────────────► │  • Idempotencia (UPC)        │
│  ES Modules + Tailwind  │                          └──────────────┬───────────────┘
└─────────────────────────┘                                         │
                                               ┌────────────────────┼────────────────────┐
                                               ▼                    ▼                    ▼
                                        PostgreSQL              MongoDB           GestoPago REST
                                  (Clientes, Cuentas,         (Catálogo y        (sendTx.do /
                                   Sesiones, 2FA, Pagos)       Logs XML)          confirmTx.do)
```

---

## Funcionalidades Principales

- **Autenticación compartida y Sesión Única:** Mismas credenciales en Web y Móvil, control estricto de sesión única respaldado en PostgreSQL (`409 Conflict` con opción de revocar la sesión anterior) y expiración/revocación inmediata en servidor (`401`).
- **Doble Factor (2FA) con Google Authenticator (TOTP):** Vinculación mediante código QR local (`otpauth://`), validación en servidor con ventana ±30s, límite de intentos, tasa máxima de solicitudes y protección anti-reuso.
- **Biometría nativa (Móvil):** Desbloqueo de sesión y autorización de pagos con huella digital o reconocimiento facial (`local_auth`) y respaldo por contraseña.
- **Catálogo y Pagos de Servicios:** Sincronización real con GestoPago, búsqueda, filtrado por compañía, control de doble clic, clave única de idempotencia (`UPC`), conciliación ante timeouts (`504`) y bloqueo anti-duplicidad.
- **Historial y Comprobantes:** Consulta paginada de operaciones exitosas del titular autenticado y detalle de comprobante digital.

---

## Inicio Rápido

- **Backend local:** `.\gradlew.bat bootRun` (requiere PostgreSQL y MongoDB activos; ver [Despliegue y Pruebas](./docs/DESPLIEGUE_Y_PRUEBAS.md)).
- **Pruebas Backend:** `.\gradlew.bat test`
- **Pruebas Flutter:** `flutter analyze && flutter test`
- **Producción Backend (HTTPS):** `https://gestopago-backend-ff40.onrender.com`
