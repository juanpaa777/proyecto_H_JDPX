package com.proyecto.servicios.dto.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para solicitar el detalle de un usuario mediante RequestBody")
public class DetalleUsuarioRequest {

    @NotNull(message = "El ID del usuario es obligatorio")
    @Schema(description = "Identificador único del usuario", example = "1")
    private Long id;
}
