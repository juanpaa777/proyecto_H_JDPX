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
@Schema(description = "DTO para solicitar la baja lógica de un cliente mediante RequestBody")
public class EliminaClienteRequest {

    @NotNull(message = "El ID del cliente es obligatorio para la baja lógica")
    @Schema(description = "Identificador del cliente a desactivar", example = "1")
    private Long id;
}
