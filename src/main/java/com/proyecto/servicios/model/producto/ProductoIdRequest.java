package com.proyecto.servicios.model.producto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoIdRequest {

    @NotNull(message = "El idProducto es obligatorio")
    private Integer idProducto;
}
