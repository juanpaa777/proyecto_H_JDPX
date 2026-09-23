package com.proyecto.servicios.entity.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "catalogo_productos")
public class ProductoDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private Integer idProducto;

    @Indexed
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

    @Builder.Default
    private Boolean activo = true;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;
}
