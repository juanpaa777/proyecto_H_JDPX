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
@Schema(description = "DTO para solicitar la baja lógica de un usuario mediante RequestBody")
public class EliminaUsuarioRequest {

    @NotNull(message = "El ID del usuario a dar de baja es obligatorio")
    @Schema(description = "Identificador único del usuario a desactivar", example = "1")
    private Long id;
}
