package com.proyecto.servicios.exception.onboarding;

public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public UsuarioNoEncontradoException(Long id) {
        super("No se encontró ningún usuario con el ID: " + id);
    }
}
