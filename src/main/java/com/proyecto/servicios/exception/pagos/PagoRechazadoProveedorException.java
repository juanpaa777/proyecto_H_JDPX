package com.proyecto.servicios.exception.pagos;

import lombok.Getter;

@Getter
public class PagoRechazadoProveedorException extends RuntimeException {
    private final String codigoProveedor;

    public PagoRechazadoProveedorException(String codigoProveedor, String message) {
        super(message);
        this.codigoProveedor = codigoProveedor;
    }
}

