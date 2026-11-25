package com.proyecto.servicios.exception.onboarding;

import lombok.Getter;

@Getter
public class SesionConflictoException extends RuntimeException {

    private final String plataformaActiva;
    private final String dispositivoActivo;
    private final String fechaInicio;

    public SesionConflictoException(String mensaje, String plataformaActiva, String dispositivoActivo, String fechaInicio) {
        super(mensaje);
        this.plataformaActiva = plataformaActiva;
        this.dispositivoActivo = dispositivoActivo;
        this.fechaInicio = fechaInicio;
    }
}

