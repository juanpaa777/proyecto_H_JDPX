# Inventario de APIs y Endpoints REST

[← Volver al índice principal](../README.md)

Todos los endpoints protegidos requieren el header:
```http
Authorization: Bearer <JWT>
Content-Type: application/json
```

---

## 1. Autenticación, 2FA y Sesiones (`/auth`)

| Endpoint | Método | Auth | Descripción | Códigos HTTP |
| :--- | :---: | :---: | :--- | :--- |
| `/auth/login` | `POST` | Pública | Valida correo/contraseña (BCrypt), detecta sesión activa (`409`) y genera desafío 2FA TOTP. | `200`, `400`, `401`, `403`, `409` |
| `/auth/verify-2fa` | `POST` | Pública | Valida código TOTP de 6 dígitos en servidor, revoca sesión previa si aplica y emite JWT. | `200`, `400`, `401`, `409` |
| `/auth/logout` | `POST` | JWT | Marca la sesión activa (`jti`) como inactiva en PostgreSQL. | `200`, `401` |
| `/auth/session-status` | `GET` | JWT | Verifica vigencia de sesión y devuelve saldo actualizado de la cuenta principal. | `200`, `401` |
| `/auth/totp-qr` | `GET` | JWT | Devuelve `otpauthUri` y `totpSecretManual` del titular autenticado. | `200`, `401` |
| `/auth/mi-cuenta` | `GET` | JWT | Devuelve datos del titular y todas sus cuentas bancarias asociadas. | `200`, `401` |

### Ejemplo `POST /auth/login`
**Request:**
```json
{
  "correo": "usuario@correo.com",
  "password": "TuPassword123!",
  "plataforma": "WEB_APP",
  "dispositivo": "Google Chrome",
  "forzarCierreSesionPrevia": false,
  "requerir2fa": true
}
```
**Response (`200 OK`):**
```json
{
  "requiere2fa": true,
  "desafioId": "uuid-desafio",
  "expira2faSegundos": 300,
  "clienteId": 40,
  "correo": "usuario@correo.com",
  "nombreCompleto": "Nombre Titular",
  "otpauthUri": "otpauth://totp/GestoPago:...",
  "totpSecretManual": "BASE32SECRET",
  "totpVinculado": false,
  "mensaje": "Escanea el código QR con Google Authenticator..."
}
```

---

## 2. Registro y Cuentas (`/clientes` y `/cuentas`)

| Endpoint | Método | Auth | Descripción | Códigos HTTP |
| :--- | :---: | :---: | :--- | :--- |
| `/clientes/verificar-disponibilidad` | `POST` | Pública | Verifica disponibilidad de `CORREO`, `CURP` o `RFC` en tiempo real. | `200`, `400` |
| `/clientes/cp` | `POST` | Pública | Consulta estado, municipio y colonias por código postal de 5 dígitos. | `200`, `400` |
| `/clientes/registrar` | `POST` | Pública | Registra cliente, domicilio, cuenta inicial y usuario con BCrypt. | `201`, `400`, `409` |
| `/cuentas/crear` | `POST` | JWT + Anti-IDOR | Crea una cuenta bancaria adicional para el titular autenticado. | `201`, `400`, `401`, `403` |
| `/cuentas/consultar` | `POST` | JWT + Anti-IDOR | Lista paginada de cuentas filtrada al `clienteId` del titular. | `200`, `401` |

---

## 3. Catálogo de Productos GestoPago (`/api/v1/productos`)

| Endpoint | Método | Auth | Descripción | Códigos HTTP |
| :--- | :---: | :---: | :--- | :--- |
| `/api/v1/productos` | `GET` | Pública | Retorna el catálogo de productos activos sincronizado en MongoDB (`page`, `size`). | `200` |
| `/api/v1/productos/sync` | `POST` | JWT / Sistema | Sincroniza el catálogo en vivo desde GestoPago hacia MongoDB. | `200`, `502` |
| `/api/v1/productos/detalle` | `POST` | Pública | Consulta detalle de un producto por `idProducto` en `@RequestBody`. | `200`, `404` |

---

## 4. Pagos de Servicios, Conciliación e Historial (`/api/v1/pagos-servicios`)

| Endpoint | Método | Auth | Descripción | Códigos HTTP |
| :--- | :---: | :---: | :--- | :--- |
| `/api/v1/pagos-servicios/pagar` | `POST` | JWT | Valida cuenta activa, saldo, contraseña BCrypt, catálogo MongoDB e idempotencia (`upc`), cifra payload AES-256 y ejecuta `sendTx.do`. | `200`, `400`, `401`, `409`, `422`, `504` |
| `/api/v1/pagos-servicios/conciliar` | `POST` | JWT | Consulta `confirmTx.do` para un `upc` en `PENDIENTE_CONCILIACION` y ajusta saldo/estatus real. | `200`, `404`, `504` |
| `/api/v1/pagos-servicios/consultar` | `POST` | JWT + Anti-IDOR | Consulta paginada de operaciones del titular autenticado (`estatus: "EXITOSA"`). | `200`, `401` |

### Ejemplo `POST /api/v1/pagos-servicios/pagar`
**Request:**
```json
{
  "numeroCuenta": "5763909804",
  "idServicio": 10,
  "idProducto": 15,
  "precio": 100.00,
  "telefonoReferencia": "5512345678",
  "bandera": "SIN_REFERENCIA",
  "pin": "TuPassword123!",
  "upc": "GP83_4910ABC123"
}
```
**Manejo de errores e idempotencia:**
- **`409 Conflict`:** `upc` duplicado o recarga al mismo número/servicio dentro de 15 minutos (24 horas para servicios con referencia).
- **`504 Gateway Timeout`:** El proveedor tardó en responder; la operación queda en `PENDIENTE_CONCILIACION` y el cliente debe invocar `/conciliar` antes de reintentar.
