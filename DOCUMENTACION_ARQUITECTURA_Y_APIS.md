# Documentación Técnica de Arquitectura, Decisiones de Seguridad e Inventario de APIs (GestoPago)

## 1. Resumen Ejecutivo y Arquitectura del Ecosistema

El ecosistema de comercio digital y banca **GestoPago** está integrado por dos aplicaciones cliente independientes (una **Aplicación Móvil en Flutter** y una **Aplicación Web Modular**) que consumen sin duplicidad la misma infraestructura de servicios REST del Backend empresarial en **Spring Boot 3**, conectado en tiempo real con **PostgreSQL**, **MongoDB** y el integrador externo **GestoPago**.

### 1.1. Arquitectura de la Aplicación Móvil (`banco_front` - Flutter)
Implementa **Clean Architecture** estricta dividida en 4 capas desacopladas:
- **`core/`**: Configuración centralizada de endpoints ([`api_endpoints.dart`](file:///c:/Users/diego%20pardo/Documents/ing%204/hasani/banco_front/lib/core/config/api_endpoints.dart)), cliente HTTP **Dio** con interceptores de token y detección de revocación de sesión ([`dio_client.dart`](file:///c:/Users/diego%20pardo/Documents/ing%204/hasani/banco_front/lib/core/network/dio_client.dart)), almacenamiento cifrado con `flutter_secure_storage` ([`secure_storage_service.dart`](file:///c:/Users/diego%20pardo/Documents/ing%204/hasani/banco_front/lib/core/storage/secure_storage_service.dart)) y jerarquía de excepciones ([`app_exceptions.dart`](file:///c:/Users/diego%20pardo/Documents/ing%204/hasani/banco_front/lib/core/error/app_exceptions.dart)).
- **`domain/`**: Entidades de negocio inmutables con `Equatable` ([`commerce_entities.dart`](file:///c:/Users/diego%20pardo/Documents/ing%204/hasani/banco_front/lib/domain/entities/commerce_entities.dart)), contratos de repositorio ([`commerce_repository.dart`](file:///c:/Users/diego%20pardo/Documents/ing%204/hasani/banco_front/lib/domain/repositories/commerce_repository.dart)) y casos de uso ([`commerce_usecases.dart`](file:///c:/Users/diego%20pardo/Documents/ing%204/hasani/banco_front/lib/domain/usecases/commerce_usecases.dart)).
- **`data/`**: Modelos con serialización JSON ([`commerce_models.dart`](file:///c:/Users/diego%20pardo/Documents/ing%204/hasani/banco_front/lib/data/models/commerce_models.dart)) e implementación de repositorios con caché en memoria para evitar llamadas HTTP redundantes ([`commerce_repository_impl.dart`](file:///c:/Users/diego%20pardo/Documents/ing%204/hasani/banco_front/lib/data/repositories/commerce_repository_impl.dart)).
- **`presentation/`**: Administración reactiva del estado mediante **BLoC / Cubit** (`AuthCubit`, `CatalogoCubit`, `CompraCubit`, `HistorialCubit`) separando el 100% de la lógica comercial de los widgets visuales ([`ecommerce_shell_screen.dart`](file:///c:/Users/diego%20pardo/Documents/ing%204/hasani/banco_front/lib/presentation/screens/ecommerce_shell_screen.dart)).

---

## 2. Justificación de Decisiones de Seguridad Informática

### 2.1. ¿Por qué el Segundo Factor (2FA) NO debe validarse únicamente en el Frontend?
1. **Vulnerabilidad de Manipulación de Cliente (Client-Side Bypass):** Cualquier validación realizada solo en JavaScript o en el cliente móvil puede ser inspeccionada, modificada con herramientas de depuración (DevTools) o evadida consumiendo directamente los endpoints REST con herramientas como Postman o `curl`.
2. **Emisión Condicionada del JWT en el Servidor:** En nuestra arquitectura ([`AuthService.java`](file:///c:/Users/diego%20pardo/Documents/ing%204/hasani/prueba/src/main/java/com/proyecto/servicios/service/onboarding/AuthService.java)), cuando el usuario envía su correo y contraseña a `POST /auth/login`, **el servidor NO entrega el token JWT**. Únicamente devuelve un identificador de desafío (`desafioId`) y almacena el hash BCrypt del código temporal OTP de 6 dígitos en la tabla `codigos_2fa` con:
   - **Expiración estricta:** 5 minutos desde su generación.
   - **Control de fuerza bruta:** Máximo 3 intentos fallidos por desafío. Al tercer fallo, el código queda inutilizado.
   - **Anti-reutilización:** Una vez validado en `POST /auth/verify-2fa`, se marca `usado = true`.
   - **Rate Limiting:** Máximo 5 solicitudes de código en una ventana de 5 minutos por usuario.

### 2.2. Exclusividad de Sesión Única Respaldada por el Servidor
El control de sesión única **no depende de una bandera local en el dispositivo**, sino de la tabla `sesiones_activas` en PostgreSQL y del filtro [`JwtSessionFilter.java`](file:///c:/Users/diego%20pardo/Documents/ing%204/hasani/prueba/src/main/java/com/proyecto/servicios/security/onboarding/JwtSessionFilter.java):
1. Cada token JWT emitido contiene un identificador único criptográfico `jti` (JWT ID) registrado en `sesiones_activas` con `activa = true`.
2. Si un usuario tiene sesión activa en **Flutter** e intenta ingresar desde la **App Web**, el backend detecta el registro activo y responde `409 Conflict` (`ACTIVE_SESSION_EXISTS`) informando la plataforma (`MOBILE_ANDROID`), el dispositivo y la hora de inicio.
3. Cuando el usuario confirma que desea finalizar la sesión anterior (`forzarCierreSesionPrevia = true`), el servidor revoca la sesión anterior (`activa = false`, `motivo_cierre = 'REEMPLAZADA_POR_SESION_EN_WEB_APP'`) y emite el nuevo `jti`.
4. En cuanto la aplicación anterior (Flutter) intenta realizar cualquier petición HTTP, `JwtSessionFilter` verifica el `jti` en base de datos, detecta que ya no está activa y responde `401 Unauthorized` (`SESSION_REVOKED_BY_OTHER_DEVICE`), cerrando automáticamente la sesión en el primer dispositivo.

### 2.3. Idempotencia, Anti-Doble-Clic y Conciliación ante Timeouts
- **Bloqueo en UI y Estado:** `CompraCubit` en Flutter y `compraProcessing` en Web bloquean el botón de compra mientras una solicitud está en vuelo.
- **Clave de Idempotencia (`UPC`):** Cada intención de compra genera una llave única (`GP83_<timestamp><random>`) validada con restricción `UNIQUE (upc)` en PostgreSQL y verificación de duplicidad temporal en [`PagoServicioService.java`](file:///c:/Users/diego%20pardo/Documents/ing%204/hasani/prueba/src/main/java/com/proyecto/servicios/service/pagos/PagoServicioService.java).
- **Conciliación de Timeouts (`confirmTx.do`):** Si la llamada a `sendTx.do` excede el tiempo de espera, la transacción se guarda como `PENDIENTE_CONCILIACION` (sin descontar saldo a ciegas ni reintentar el cobro) y el servidor responde `504 Gateway Timeout`. La interfaz presenta el botón **"Conciliar Estado Real"**, que invoca `POST /api/v1/pagos-servicios/conciliar` (`confirmTx.do`) para verificar si el proveedor aplicó el pago (código `06`) o no lo aplicó (código `70`).

---

## 3. Inventario de Servicios REST Consumidos

### 3.1. Servicios Internos del Backend Ecosistema (`http://localhost:8080`)

| Nombre del Servicio | Método y URL | Headers | JSON Entrada | JSON Respuesta | Códigos HTTP |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1. Inicio de Sesión y Detección de Sesión** | `POST /auth/login` | `Content-Type: application/json` | `{"correo": "...", "password": "...", "plataforma": "WEB_APP", "dispositivo": "...", "forzarCierreSesionPrevia": false, "requerir2fa": true}` | `{"requiere2fa": true, "desafioId": "uuid", "codigoOtpDemo": "123456", "expira2faSegundos": 300, "correo": "...", "nombreCompleto": "..."}` | `200 OK`, `401 Unauthorized`, `403 Forbidden`, `409 Conflict` (`ACTIVE_SESSION_EXISTS`) |
| **2. Verificación de Segundo Factor (2FA)** | `POST /auth/verify-2fa` | `Content-Type: application/json` | `{"desafioId": "uuid", "codigoOtp": "123456", "forzarCierreSesionPrevia": true}` | `{"token": "eyJ...", "tipo": "Bearer", "sessionId": "jti", "clienteId": 1, "numeroCuenta": "4028193847", "saldo": 1500.00}` | `200 OK`, `401 Unauthorized` (código inválido/expirado/3 intentos) |
| **3. Estado de Sesión Activa** | `GET /auth/session-status` | `Authorization: Bearer <JWT>` | *(Sin cuerpo)* | `{"activa": true, "sessionId": "jti", "clienteId": 1, "numeroCuenta": "...", "saldo": 1500.00}` | `200 OK`, `401 Unauthorized` (`SESSION_REVOKED_BY_OTHER_DEVICE`) |
| **4. Cierre de Sesión Seguro** | `POST /auth/logout` | `Authorization: Bearer <JWT>` | *(Sin cuerpo)* | `{"codigo": 200, "mensaje": "Sesión finalizada correctamente"}` | `200 OK` |
| **5. Consulta de Catálogo GestoPago** | `GET /api/v1/productos` | `Authorization: Bearer <JWT>` | *(Sin cuerpo)* | `[{"idProducto": 205, "idServicio": 76, "servicio": "Telcel", "producto": "Recarga 100", "tipoFront": 1, "precio": 100.00, "activo": true}]` | `200 OK`, `401 Unauthorized` |
| **6. Sincronización Manual Catálogo** | `POST /api/v1/productos/sync` | `Authorization: Bearer <JWT>` | *(Sin cuerpo)* | `{"estado": "EXITO", "totalProductos": 120}` | `200 OK`, `503 Service Unavailable` |
| **7. Procesamiento de Compra** | `POST /api/v1/pagos-servicios/pagar` | `Authorization: Bearer <JWT>` | `{"numeroCuenta": "4028193847", "idServicio": 76, "idProducto": 205, "precio": 100.00, "telefonoReferencia": "5512345678", "bandera": "SIN_REFERENCIA", "pin": "Pass123!", "upc": "GP83_123ABC"}` | `{"id": 1, "upc": "GP83_123ABC", "estatus": "EXITOSA", "codigoRespuesta": "01", "numAutorizacion": "...", "saldoAnterior": 1500.0, "saldoPosterior": 1400.0}` | `200 OK`, `400 Bad Request`, `409 Conflict` (duplicado), `422 Unprocessable Entity` (saldo/rechazo), `504 Gateway Timeout` |
| **8. Conciliación de Timeout** | `POST /api/v1/pagos-servicios/conciliar` | `Authorization: Bearer <JWT>` | `{"upc": "GP83_123ABC"}` | `{"upc": "GP83_123ABC", "estatus": "EXITOSA", "codigoRespuesta": "06", "saldoPosterior": 1400.0}` | `200 OK`, `404 Not Found` |
| **9. Historial de Compras Exitosas** | `POST /api/v1/pagos-servicios/consultar` | `Authorization: Bearer <JWT>` | `{"clienteId": 1, "numeroCuenta": "4028193847", "estatus": "EXITOSA", "page": 0, "size": 8}` | `{"contenido": [...], "paginaActual": 0, "totalPaginas": 1, "totalElementos": 5}` | `200 OK`, `401 Unauthorized` |

### 3.2. Servicios Externos Consumidos de GestoPago (`https://gestopago.portalventas.net`)
1. **Autenticación de Distribuidor (`POST /sistema/rest/servicio/authenticate.do`):** Obtiene el token de distribuidor renovado automáticamente.
2. **Descarga de Catálogo (`GET /sistema/rest/servicio/getProducts.do`):** Retorna el XML de productos y servicios sincronizado en MongoDB.
3. **Ejecución de Transacción (`POST /sistema/rest/servicio/sendTx.do`):** Recibe el payload JSON cifrado con **AES-256/CBC/PKCS5Padding** y responde XML con código `01` (éxito).
4. **Confirmación / Conciliación (`POST /sistema/rest/servicio/confirmTx.do`):** Verifica el estado real de una operación tras un timeout (`06` aplicada / `70` no aplicada).

