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
public class VerificarDisponibilidadRequest {

    @NotBlank(message = "El campo a verificar es obligatorio")
    @Pattern(regexp = "^(CORREO|CURP|RFC)$", message = "Campo inválido. Use CORREO, CURP o RFC")
    private String campo;

    @NotBlank(message = "El valor a verificar es obligatorio")
    @Size(max = 254, message = "El valor excede el límite permitido")
    private String valor;
}
