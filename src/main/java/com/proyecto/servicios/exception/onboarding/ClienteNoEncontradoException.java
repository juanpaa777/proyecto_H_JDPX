package com.proyecto.servicios.exception.onboarding;

public class ClienteNoEncontradoException extends RuntimeException {
    public ClienteNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public ClienteNoEncontradoException(Long id) {
        super("No se encontró el cliente con ID: " + id);
    }
}
