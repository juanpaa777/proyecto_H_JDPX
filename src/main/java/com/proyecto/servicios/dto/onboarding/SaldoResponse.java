package com.proyecto.servicios.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaldoResponse {

    private String numeroCuenta;
    private BigDecimal saldo;
    private String estatus;
}
