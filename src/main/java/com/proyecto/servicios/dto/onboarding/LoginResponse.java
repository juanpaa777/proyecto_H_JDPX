package com.proyecto.servicios.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tipo = "Bearer";
    private Long expiresIn;
    private Long clienteId;
    private String correo;
    private String nombreCompleto;

    public LoginResponse(String token, Long expiresIn, Long clienteId, String correo, String nombreCompleto) {
        this.token = token;
        this.tipo = "Bearer";
        this.expiresIn = expiresIn;
        this.clienteId = clienteId;
        this.correo = correo;
        this.nombreCompleto = nombreCompleto;
    }
}
