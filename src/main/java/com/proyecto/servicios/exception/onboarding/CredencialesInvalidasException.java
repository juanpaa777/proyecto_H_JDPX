package com.proyecto.servicios.exception.onboarding;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }

    public CredencialesInvalidasException() {
        super("Credenciales de acceso inválidas. Verifique su correo y contraseña.");
    }
}
