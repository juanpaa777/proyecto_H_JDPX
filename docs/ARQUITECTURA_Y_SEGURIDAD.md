# Arquitectura de Software y Seguridad Informática

[← Volver al índice principal](../README.md)

---

## 1. Arquitectura de la Aplicación Móvil (Flutter)

La aplicación móvil (`banco_front`) sigue **Clean Architecture** dividida por responsabilidades:

| Capa | Responsabilidad | Componentes clave |
| :--- | :--- | :--- |
| **Presentation** | UI, widgets reactivos y estado | `EcommerceShellScreen`, `SplashScreen`, `RegistroScreen`, `AuthCubit`, `CatalogoCubit`, `CompraCubit`, `HistorialCubit` |
| **Domain** | Entidades puras y contratos | `UserSession`, `ProductoEntity`, `TransaccionEntity`, `CommerceRepository` |
| **Data** | Implementación de repositorios y modelos JSON | `CommerceRepositoryImpl`, `UserSessionModel`, `ProductoModel`, `TransaccionModel` |
| **Core** | Red, seguridad, almacenamiento y utilidades | `DioClient`, `ApiEndpoints`, `SecureStorageService`, `BiometricService`, `BrandLogoHelper`, `AppException` |

### Administración de Estado y Capa de Red
- **BLoC / Cubit:** Separa por completo la lógica de negocio de los widgets, maneja estados de carga (`AuthLoading`, `CompraProcessing`), errores y control de operaciones simultáneas.
- **DioClient:** Centraliza `baseUrl`, timeouts (`60s`/`70s`), inyección automática de `Authorization: Bearer <JWT>` y un interceptor `onError` que detecta respuestas `401` (`SESSION_REVOKED`) para cerrar la sesión local de inmediato.

---

## 2. Decisiones de Seguridad Implementadas

### Exclusividad de Sesión Única en Servidor
- Cada inicio de sesión válido genera un `jti` único almacenado en la tabla `sesiones_activas` de PostgreSQL.
- `JwtSessionFilter` valida en **cada petición** que el `jti` del token siga activo en base de datos y no haya expirado.
- Si el usuario intenta ingresar desde otra plataforma (Web ↔ Móvil) con una sesión activa, el servidor responde **`409 Conflict`** (`ACTIVE_SESSION_EXISTS`). Solo al confirmar el cierre de la sesión previa se revoca el `jti` anterior en PostgreSQL y se emite el nuevo JWT.

### ¿Por qué el 2FA (TOTP) se valida en el Servidor y nunca en el Frontend?
- El secreto TOTP (`totp_secret`) y el último paso validado (`totp_ultimo_paso`) residen exclusivamente en PostgreSQL (`AuthService` + `TotpService`).
- Validar en frontend permitiría a un atacante omitir la pantalla 2FA manipulando el estado local o llamando directamente al endpoint de sesión.
- El servidor garantiza:
  1. Expiración del desafío (5 minutos).
  2. Máximo 3 intentos fallidos por desafío.
  3. Protección contra reutilización del mismo código dentro de la ventana de 30s (`totp_ultimo_paso`).
  4. Rate limiting (máximo de solicitudes en 5 minutos).

### Control de Acceso y Anti-IDOR
- `JwtSessionFilter` bloquea con `401 Unauthorized` cualquier acceso sin token válido a rutas protegidas (`/cuentas/**`, `/clientes/consultar`, `/clientes/detalle`, `/clientes/actualizar`, `/clientes/baja`, `/api/v1/pagos-servicios/**`, `/auth/mi-cuenta`, `/auth/totp-qr`, `/auth/session-status`).
- `AuthenticatedClientGuard` verifica que el `clienteId` solicitado coincida con el titular del JWT autenticado, impidiendo que un usuario consulte o modifique cuentas o historiales de terceros.
- El endpoint `GET /auth/mi-cuenta` resuelve el titular directamente desde el JWT sin aceptar parámetros externos.

### Manejo Seguro de Errores y Almacenamiento
- `GlobalExceptionHandler` registra el detalle técnico únicamente en los logs del servidor y devuelve mensajes genéricos al cliente (evitando enumeración de usuarios, fuga de campos, rutas internas o trazas SQL).
- **Flutter:** Tokens y secretos se almacenan cifrados con `flutter_secure_storage` (Keystore/Keychain). Ningún formulario inicia con credenciales precargadas.
- **Web:** `sessionStorage` para sesión activa, `escapeHtml()` contra XSS, QR TOTP generado 100% en cliente y `autocomplete="off"` / `"new-password"` en campos sensibles.
