package com.proyecto.servicios.dto.pagos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para ejecutar un pago de servicio o recarga mediante GestoPago (100% RequestBody)")
public class PagoServicioRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = "^[0-9]{10,20}$", message = "El número de cuenta debe contener entre 10 y 20 dígitos numéricos")
    @Schema(description = "Número de cuenta bancaria activa del cliente", example = "4028193847")
    private String numeroCuenta;

    @NotNull(message = "El ID del servicio es obligatorio")
    @Min(value = 1, message = "El ID del servicio debe ser un entero positivo")
    @Schema(description = "Identificador del servicio obtenido del catálogo", example = "76")
    private Integer idServicio;

    @NotNull(message = "El ID del producto es obligatorio")
    @Min(value = 1, message = "El ID del producto debe ser un entero positivo")
    @Schema(description = "Identificador del producto obtenido del catálogo", example = "205")
    private Integer idProducto;

    @NotNull(message = "El precio o monto del pago es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto del pago debe ser mayor a cero")
    @DecimalMax(value = "10000.00", message = "El monto del pago no puede exceder $10,000.00")
    @Schema(description = "Precio o monto a pagar del servicio", example = "366.99")
    private BigDecimal precio;

    @Schema(description = "Identificador del distribuidor (opcional, usa el configurado por defecto)", example = "83")
    private Integer distribuidor;

    @NotBlank(message = "El teléfono o referencia de pago es obligatorio")
    @Size(min = 4, max = 100, message = "El teléfono o referencia debe tener entre 4 y 100 caracteres")
    @Schema(description = "Número telefónico (10 dígitos) o referencia del recibo de servicio", example = "25720381")
    private String telefonoReferencia;

    @NotBlank(message = "La bandera de tipo de pago es obligatoria (CON_REFERENCIA o SIN_REFERENCIA)")
    @Pattern(
            regexp = "^(?i)(CON_REFERENCIA|SIN_REFERENCIA|1|2)$",
            message = "La bandera debe ser CON_REFERENCIA, SIN_REFERENCIA, 1 (sin referencia) o 2 (con referencia)"
    )
    @Schema(description = "Indicador de modalidad de pago: CON_REFERENCIA o SIN_REFERENCIA", example = "CON_REFERENCIA")
    private String bandera;

    @NotBlank(message = "El PIN o contraseña de autorización del cliente es obligatorio")
    @Schema(description = "Contraseña o PIN seguro del usuario titular de la cuenta para autorizar el cargo", example = "Password123!")
    private String pin;

    @Size(min = 3, max = 15, message = "El código UPC debe tener entre 3 y 15 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9_\\-]{3,15}$", message = "El código UPC solo puede contener caracteres alfanuméricos, guiones y guion bajo")
    @Schema(description = "Código único de operación (UPC) opcional para idempotencia; si se omite se genera automáticamente", example = "UPC83_A1B2C3")
    private String upc;
}

