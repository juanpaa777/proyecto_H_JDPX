package com.proyecto.servicios.dto.onboarding;

import com.proyecto.servicios.validation.onboarding.PasswordSeguro;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para la actualización de un usuario existente mediante RequestBody")
public class UsuarioUpdateRequest {

    @NotNull(message = "El ID del usuario es obligatorio para la actualización")
    @Schema(description = "Identificador del usuario", example = "1")
    private Long id;

    @Email(message = "El formato de correo electrónico debe ser válido")
    @Schema(description = "Nuevo correo electrónico", example = "nuevo.correo@banco.com")
    private String correo;

    @PasswordSeguro
    @Schema(description = "Nueva contraseña cifrada", example = "NuevaClave123!")
    private String password;

    @Schema(description = "Estatus activo del usuario", example = "true")
    private Boolean activo;
}
