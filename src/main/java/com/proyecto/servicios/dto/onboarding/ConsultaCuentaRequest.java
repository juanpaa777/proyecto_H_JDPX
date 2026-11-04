package com.proyecto.servicios.dto.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para consulta dinámica y paginada de cuentas mediante RequestBody")
public class ConsultaCuentaRequest {

    @Schema(description = "ID del cliente titular (opcional)", example = "1")
    private Long clienteId;

    @Schema(description = "Estatus de la cuenta (ej. ACTIVA, INACTIVA) (opcional)", example = "ACTIVA")
    private String estatus;

    @Min(value = 0, message = "El número de página no puede ser menor a 0")
    @Schema(description = "Número de página solicitada (índice base 0)", example = "0", defaultValue = "0")
    @Builder.Default
    private Integer page = 0;

    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    @Schema(description = "Cantidad de registros por página", example = "10", defaultValue = "10")
    @Builder.Default
    private Integer size = 10;
}
