# Despliegue, Ambientes y Pruebas

[← Volver al índice principal](../README.md)

---

## 1. Separación de Ambientes (Desarrollo vs Producción)

| Componente | Ambiente Local (Desarrollo) | Ambiente Producción |
| :--- | :--- | :--- |
| **Backend Spring Boot** | `http://localhost:8080` (Docker PostgreSQL `:5434` + MongoDB `:27017`) | `https://gestopago-backend-ff40.onrender.com` (Render, rama `develop`, HTTPS) |
| **Aplicación Web** | `http://localhost:5173` (detecta `localhost` y apunta a `:8080`) | Vercel (detecta dominio público y apunta por HTTPS a Render) |
| **App Móvil Flutter** | `_useLocalBackend = true` en `api_endpoints.dart` | `_useLocalBackend = false` (HTTPS a Render) |

---

## 2. Variables de Entorno del Backend

Configuradas mediante variables de entorno (sin secretos expuestos en el código fuente):

- `SPRING_DATASOURCE_URL`, `PGUSER`, `PGPASSWORD` (PostgreSQL + Flyway)
- `SPRING_DATA_MONGODB_URI` (MongoDB catálogo y bitácora XML)
- `JWT_SECRET` (firma HMAC-SHA256 de tokens)
- `GESTOPAGO_AUTH_URL`, `GESTOPAGO_CATALOGO_URL`, `GESTOPAGO_TX_URL`, `GESTOPAGO_ID_DISTRIBUIDOR`, `GESTOPAGO_CODIGO_DISPOSITIVO`, `GESTOPAGO_PASSWORD`

---

## 3. Ejecución de Pruebas y Análisis Estático

### Backend (`prueba`)
```powershell
.\gradlew.bat test
```
Valida pruebas unitarias y de controladores (`AuthServiceTest`, `PagoServicioServiceTest`, `ClienteControllerTest`, `CuentaControllerTest`, `UsuarioControllerTest`, etc.).

### Aplicación Móvil (`banco_front`)
```powershell
flutter analyze
flutter test
flutter build apk --release
```
Valida análisis estático, pruebas de Cubits/repositorios (`AuthCubit`, `CatalogoCubit`, `CompraCubit`) y validadores de registro (`RegistroValidators`), además de generar `build/app/outputs/flutter-apk/app-release.apk`.
