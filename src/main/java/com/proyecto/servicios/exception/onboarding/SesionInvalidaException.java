package com.proyecto.servicios.exception.onboarding;

import lombok.Getter;

@Getter
public class SesionInvalidaException extends RuntimeException {

    private final String codigoError;

    public SesionInvalidaException(String mensaje, String codigoError) {
        super(mensaje);
        this.codigoError = codigoError;
    }
}

