package com.proyecto.servicios.entity.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "catalogo_sincronizacion_logs")
public class CatalogoSyncLogDocument {

    @Id
    private String id;

    private LocalDateTime fechaEjecucion;

    private String estado;

    private Integer codigoRespuesta;

    private String mensajeRespuesta;

    private Integer totalProductos;

    private Long tiempoEjecucionMs;

    private String xmlRaw;

    private String errorDetalle;
}
