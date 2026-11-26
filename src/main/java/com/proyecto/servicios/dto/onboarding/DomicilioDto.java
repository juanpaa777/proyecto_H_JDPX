package com.proyecto.servicios.dto.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DomicilioDto {

    private Long id;

    @NotBlank(message = "La calle es obligatoria")
    @Pattern(regexp = "^[\\p{L}0-9\\s.,#'\\-/°]{2,100}$", message = "La calle solo puede contener letras, números, espacios y puntuación básica (2 a 100 caracteres)")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    @Pattern(regexp = "^[\\p{L}0-9\\s\\-/]{1,20}$", message = "El número exterior solo acepta letras, números, espacios, guion o '/' (ej. 12-A o S/N)")
    private String numeroExterior;

    @Pattern(regexp = "^$|^[\\p{L}0-9\\s\\-/]{1,20}$", message = "El número interior solo acepta letras, números, espacios, guion o '/' (máx. 20 caracteres)")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 80, message = "La colonia no puede exceder 80 caracteres")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    @Size(max = 80, message = "El municipio no puede exceder 80 caracteres")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 50, message = "El estado no puede exceder 50 caracteres")
    private String estado;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe contener exactamente 5 dígitos")
    private String codigoPostal;

    @NotBlank(message = "El país es obligatorio")
    @Size(max = 50, message = "El país no puede exceder 50 caracteres")
    private String pais = "México";
}