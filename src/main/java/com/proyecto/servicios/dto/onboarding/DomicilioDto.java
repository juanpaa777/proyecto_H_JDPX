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
    @Size(max = 100, message = "La calle no puede exceder 100 caracteres")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    @Size(max = 20, message = "El número exterior no puede exceder 20 caracteres")
    private String numeroExterior;

    @Size(max = 20, message = "El número interior no puede exceder 20 caracteres")
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