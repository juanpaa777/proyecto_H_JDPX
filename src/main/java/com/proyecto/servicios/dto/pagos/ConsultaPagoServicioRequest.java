package com.proyecto.servicios.dto.pagos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Criterios combinables para consulta dinámica y paginada de pagos de servicios mediante RequestBody")
public class ConsultaPagoServicioRequest {

    @Schema(description = "ID interno del pago (opcional)", example = "1")
    private Long id;

    @Schema(description = "ID del cliente titular (opcional)", example = "1")
    private Long clienteId;

    @Schema(description = "Número de cuenta bancaria (opcional)", example = "4028193847")
    private String numeroCuenta;

    @Schema(description = "Código único de operación UPC (opcional)", example = "UPC83_A1B2C3")
    private String upc;

    @Schema(description = "ID del servicio (opcional)", example = "76")
    private Integer idServicio;

    @Schema(description = "ID del producto (opcional)", example = "205")
    private Integer idProducto;

    @Schema(description = "Estatus de la transacción (EXITOSA, RECHAZADA, PENDIENTE_CONCILIACION) (opcional)", example = "EXITOSA")
    private String estatus;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Fecha inicial del rango de consulta", example = "2026-01-01T00:00:00")
    private LocalDateTime fechaInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Fecha final del rango de consulta", example = "2026-12-31T23:59:59")
    private LocalDateTime fechaFin;

    @Min(value = 0, message = "El número de página no puede ser menor a 0")
    @Schema(description = "Número de página solicitada (índice base 0)", example = "0", defaultValue = "0")
    @Builder.Default
    private Integer page = 0;

    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    @Schema(description = "Cantidad de registros por página", example = "10", defaultValue = "10")
    @Builder.Default
    private Integer size = 10;
}

