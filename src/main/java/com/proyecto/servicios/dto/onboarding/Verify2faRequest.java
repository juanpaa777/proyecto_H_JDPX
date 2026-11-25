package com.proyecto.servicios.dto.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Verify2faRequest {

    @NotBlank(message = "El identificador del desafío 2FA es obligatorio")
    private String desafioId;

    @NotBlank(message = "El código de verificación 2FA es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El código 2FA debe contener exactamente 6 dígitos numéricos")
    private String codigoOtp;

    private Boolean forzarCierreSesionPrevia;
}

