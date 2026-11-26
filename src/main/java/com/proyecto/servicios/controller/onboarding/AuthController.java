package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.dto.onboarding.ClienteResponse;
import com.proyecto.servicios.dto.onboarding.LoginRequest;
import com.proyecto.servicios.dto.onboarding.LoginResponse;
import com.proyecto.servicios.dto.onboarding.SessionStatusResponse;
import com.proyecto.servicios.dto.onboarding.Verify2faRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.onboarding.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación, 2FA y Control de Sesiones", description = "Endpoints de autenticación con BCrypt, doble factor (2FA) y exclusividad de sesión activa en servidor")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "Inicio de sesión (Paso 1: Credenciales y Detección de Sesión Activa)",
            description = "Valida correo y contraseña con BCrypt, verifica si existe otra sesión activa en otro dispositivo (409 Conflict) y genera el desafío 2FA de 6 dígitos."
    )
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.autenticarUsuario(request));
    }

    @PostMapping("/verify-2fa")
    @Operation(
            summary = "Verificación de Segundo Factor (Paso 2: Validación OTP en Servidor y Emisión de JWT)",
            description = "Valida el código temporal de 6 dígitos en el servidor (expiración 5 min, máx 3 intentos, no reutilizable), revoca sesiones previas si se autorizó y emite el JWT definitivo."
    )
    public ResponseEntity<LoginResponse> verify2fa(@Valid @RequestBody Verify2faRequest request) {
        return ResponseEntity.ok(authService.verificarCodigo2fa(request));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Cerrar sesión activa de forma segura",
            description = "Invalida inmediatamente la sesión actual en el servidor PostgreSQL para impedir el uso posterior del token."
    )
    public ResponseEntity<GenericResponse> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.cerrarSesion(authHeader);
        return ResponseEntity.ok(new GenericResponse(200, "Sesión finalizada correctamente en el servidor."));
    }

    @GetMapping("/session-status")
    @Operation(
            summary = "Consultar estado de la sesión actual y saldo actualizado",
            description = "Verifica en el servidor si el token JWT sigue perteneciendo a una sesión activa y devuelve los datos de la cuenta del usuario."
    )
    public ResponseEntity<SessionStatusResponse> sessionStatus(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(authService.consultarEstadoSesion(authHeader));
    }

    @GetMapping("/totp-qr")
    @Operation(
            summary = "Obtener QR y clave TOTP para vincular Google Authenticator",
            description = "Devuelve el URI otpauth y clave manual del usuario autenticado para vincular o recuperar Google Authenticator."
    )
    public ResponseEntity<LoginResponse> getTotpQr(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(authService.obtenerTotpQrAutenticado(authHeader));
    }

    @PostMapping("/totp-reset")
    @Operation(
            summary = "Regenerar QR y clave TOTP para vincular en otro dispositivo",
            description = "Genera una nueva clave secreta TOTP y reinicia la vinculación de Google Authenticator para el usuario autenticado."
    )
    public ResponseEntity<LoginResponse> resetTotpQr(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(authService.regenerarTotpQrAutenticado(authHeader));
    }

    @GetMapping("/mi-cuenta")
    @Operation(
            summary = "Obtener datos y cuentas del titular autenticado",
            description = "Devuelve únicamente la información y cuentas bancarias del cliente asociado al JWT activo."
    )
    public ResponseEntity<ClienteResponse> getMiCuenta(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(authService.obtenerMiCuentaAutenticada(authHeader));
    }
}
