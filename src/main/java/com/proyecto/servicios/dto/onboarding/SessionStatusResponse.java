package com.proyecto.servicios.dto.onboarding;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionStatusResponse {

    private boolean activa;
    private String sessionId;
    private Long clienteId;
    private String correo;
    private String nombreCompleto;
    private String numeroCuenta;
    private BigDecimal saldo;
    private String plataforma;
    private String dispositivo;
    private String fechaInicio;
    private String mensaje;
}

