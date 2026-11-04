package com.proyecto.servicios.dto.onboarding;

import com.proyecto.servicios.validation.onboarding.PasswordSeguro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UsuarioCreateRequest {

    @NotNull(message = "El clienteId es obligatorio")
    private Long clienteId;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo electrónico debe ser válido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @PasswordSeguro
    private String password;
}
