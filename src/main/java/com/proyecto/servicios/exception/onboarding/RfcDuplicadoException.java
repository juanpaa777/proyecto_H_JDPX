package com.proyecto.servicios.exception.onboarding;

public class RfcDuplicadoException extends RuntimeException {
    public RfcDuplicadoException(String rfc) {
        super("Ya existe un cliente registrado con el RFC: " + rfc);
    }
}
