package com.proyecto.servicios.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CuentaResponse {

    private Long id;
    private Long clienteId;
    private String numeroCuenta;
    private BigDecimal saldo;
    private String estatus;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
