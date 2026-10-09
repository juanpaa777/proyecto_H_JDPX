package com.proyecto.servicios.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodigoPostalResponse {

    private String codigoPostal;
    private String estado;
    private String municipio;
    private String ciudad;
    private String pais;
    private List<String> colonias;
}
