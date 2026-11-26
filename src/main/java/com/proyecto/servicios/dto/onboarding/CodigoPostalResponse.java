package com.proyecto.servicios.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodigoPostalResponse {
    private boolean encontrado;
    private String codigoPostal;
    private String estado;
    private String municipio;
    private String ciudad;
    private String pais;
    private List<String> colonias;
    private String mensaje;
}
