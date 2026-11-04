package com.proyecto.servicios.model.producto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicioIdRequest {

    @NotNull(message = "El idServicio es obligatorio")
    private Integer idServicio;
}
