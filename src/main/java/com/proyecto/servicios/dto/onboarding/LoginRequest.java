package com.proyecto.servicios.dto.onboarding;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe ser válido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    private String plataforma;

    private String dispositivo;

    private Boolean forzarCierreSesionPrevia;

    private Boolean requerir2fa;
}
