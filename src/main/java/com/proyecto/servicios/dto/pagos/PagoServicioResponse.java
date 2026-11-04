package com.proyecto.servicios.dto.pagos;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Comprobante y detalle de resultado de un pago de servicio")
public class PagoServicioResponse {

    private Long id;
    private Long clienteId;
    private String numeroCuenta;
    private String upc;
    private Integer idServicio;
    private String nombreServicio;
    private Integer idProducto;
    private String nombreProducto;
    private String bandera;
    private String telefono;
    private String referencia;
    private BigDecimal montoPago;
    private BigDecimal comision;
    private BigDecimal saldoAnterior;
    private BigDecimal saldoPosterior;
    private String estatus;
    private String codigoRespuesta;
    private String mensajeRespuesta;
    private String idTxGestopago;
    private String numAutorizacion;
    private String pin;
    private String leyenda;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}

