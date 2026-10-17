package com.proyecto.servicios.dto.onboarding;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CuentaUpdateRequest {

    @NotBlank(message = "El número de cuenta es obligatorio para identificar la cuenta")
    private String numeroCuenta;

    @Pattern(regexp = "^(?i)(ACTIVA|INACTIVA|BLOQUEADA)$", message = "El estatus debe ser ACTIVA, INACTIVA o BLOQUEADA")
    private String estatus;

    @DecimalMin(value = "0.00", message = "El saldo no puede ser negativo")
    private BigDecimal saldo;
}
