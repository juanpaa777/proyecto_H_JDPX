package com.proyecto.servicios.exception.onboarding;

public class CorreoDuplicadoException extends RuntimeException {
    public CorreoDuplicadoException(String correo) {
        super("Ya existe un registro con el correo electrónico: " + correo);
    }
}
