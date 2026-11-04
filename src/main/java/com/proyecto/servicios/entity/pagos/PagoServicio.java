package com.proyecto.servicios.entity.pagos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos_servicios", indexes = {
        @Index(name = "idx_pagos_cuenta_id", columnList = "cuenta_id"),
        @Index(name = "idx_pagos_cliente_id", columnList = "cliente_id"),
        @Index(name = "idx_pagos_estatus", columnList = "estatus"),
        @Index(name = "idx_pagos_duplicados_servicio", columnList = "id_servicio, referencia, monto_pago, fecha_creacion"),
        @Index(name = "idx_pagos_duplicados_recarga", columnList = "id_servicio, telefono, fecha_creacion")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnore
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    @JsonIgnore
    private Cuenta cuenta;

    @Column(name = "numero_cuenta", nullable = false, length = 20)
    private String numeroCuenta;

    @Column(name = "upc", nullable = false, unique = true, length = 15)
    private String upc;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "nombre_servicio", length = 150)
    private String nombreServicio;

    @Column(name = "nombre_producto", length = 150)
    private String nombreProducto;

    @Column(name = "tipo_front")
    private Integer tipoFront;

    @Column(name = "bandera", nullable = false, length = 25)
    private String bandera;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "referencia", length = 100)
    private String referencia;

    @Column(name = "monto_pago", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoPago;

    @Column(name = "comision", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal comision = BigDecimal.ZERO;

    @Column(name = "saldo_anterior", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_posterior", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoPosterior;

    @Column(name = "hora_local", nullable = false, length = 15)
    private String horaLocal;

    @Column(name = "unidad", nullable = false, length = 25)
    private String unidad;

    @Column(name = "random_param", nullable = false, length = 50)
    private String randomParam;

    @Column(name = "payload_signed", nullable = false, columnDefinition = "TEXT")
    @JsonIgnore
    private String payloadSigned;

    @Column(name = "estatus", nullable = false, length = 30)
    private String estatus;

    @Column(name = "codigo_respuesta", length = 10)
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", length = 500)
    private String mensajeRespuesta;

    @Column(name = "id_tx_gestopago", length = 50)
    private String idTxGestopago;

    @Column(name = "num_autorizacion", length = 60)
    private String numAutorizacion;

    @Column(name = "pin", length = 250)
    private String pin;

    @Column(name = "leyenda", columnDefinition = "TEXT")
    private String leyenda;

    @Column(name = "xml_response", columnDefinition = "TEXT")
    @JsonIgnore
    private String xmlResponse;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}

