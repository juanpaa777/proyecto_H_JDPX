package com.proyecto.servicios.exception.pagos;

public class TransaccionDuplicadaException extends RuntimeException {
    public TransaccionDuplicadaException(String message) {
        super(message);
    }
}

