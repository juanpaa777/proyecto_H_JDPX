package com.proyecto.servicios.exception.pagos;

import lombok.Getter;

@Getter
public class ProveedorExternoTimeoutException extends RuntimeException {
    private final String upc;

    public ProveedorExternoTimeoutException(String upc, String message) {
        super(message);
        this.upc = upc;
    }
}

