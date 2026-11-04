package com.proyecto.servicios.exception.onboarding;

public class CuentaNoEncontradaException extends RuntimeException {
    public CuentaNoEncontradaException(String numeroCuenta) {
        super("No se encontró la cuenta bancaria con número: " + numeroCuenta);
    }
}
