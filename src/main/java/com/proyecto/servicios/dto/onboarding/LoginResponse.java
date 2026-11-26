package com.proyecto.servicios.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    @Builder.Default
    private String tipo = "Bearer";
    private Long expiresIn;
    private Long clienteId;
    private String correo;
    private String nombreCompleto;
    private String numeroCuenta;
    private BigDecimal saldo;
    private String sessionId;
    private Boolean requiere2fa;
    private String desafioId;
    private String codigoOtpDemo;
    private Long expira2faSegundos;
    private String mensaje;
    private String otpauthUri;
    private String totpSecretManual;
    private Boolean totpVinculado;

    public LoginResponse(String token, Long expiresIn, Long clienteId, String correo, String nombreCompleto) {
        this.token = token;
        this.tipo = "Bearer";
        this.expiresIn = expiresIn;
        this.clienteId = clienteId;
        this.correo = correo;
        this.nombreCompleto = nombreCompleto;
        this.requiere2fa = false;
    }
}
