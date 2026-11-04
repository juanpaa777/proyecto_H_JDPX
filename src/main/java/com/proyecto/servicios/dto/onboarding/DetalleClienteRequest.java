package com.proyecto.servicios.dto.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para solicitar el detalle de un cliente por ID, CURP, RFC o Número de cuenta")
public class DetalleClienteRequest {

    @Schema(description = "Identificador único del cliente", example = "1")
    private Long id;

    @Schema(description = "CURP del cliente", example = "PELJ900101HDFRRN01")
    private String curp;

    @Schema(description = "RFC del cliente", example = "PELJ9001018X4")
    private String rfc;

    @Schema(description = "Número de cuenta bancaria", example = "9201837465")
    private String numeroCuenta;
}
