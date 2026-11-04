package com.proyecto.servicios.model.producto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductoDto {

    private String id;
    private Integer idProducto;
    private Integer idServicio;
    private String servicio;
    private String producto;
    private Integer idCatTipoServicio;
    private Integer tipoFront;
    private Boolean hasDigitoVerificador;
    private BigDecimal precio;
    private Boolean showAyuda;
    private String tipoReferencia;
    private String legend;
    private Boolean activo;
    private LocalDateTime fechaActualizacion;
}
