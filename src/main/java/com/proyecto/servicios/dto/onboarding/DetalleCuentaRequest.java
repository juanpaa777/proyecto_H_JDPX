package com.proyecto.servicios.dto.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para consultar detalle o saldo de cuenta mediante RequestBody")
public class DetalleCuentaRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Schema(description = "Número único de cuenta bancaria", example = "9201837465")
    private String numeroCuenta;
}
