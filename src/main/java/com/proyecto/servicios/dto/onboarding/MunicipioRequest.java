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
@Schema(description = "DTO para consultar municipios por estado mediante RequestBody")
public class MunicipioRequest {

    @NotBlank(message = "El nombre del estado es obligatorio")
    @Schema(description = "Nombre del estado", example = "Guanajuato")
    private String estado;
}
