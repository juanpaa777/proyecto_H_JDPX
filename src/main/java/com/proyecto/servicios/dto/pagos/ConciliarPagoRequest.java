package com.proyecto.servicios.dto.pagos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para conciliar una transacción pendiente por timeout mediante confirmTx.do")
public class ConciliarPagoRequest {

    @NotBlank(message = "El código UPC de la transacción es obligatorio")
    @Size(min = 3, max = 15, message = "El código UPC debe tener entre 3 y 15 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9_\\-]{3,15}$", message = "El código UPC solo puede contener caracteres alfanuméricos, guiones y guion bajo")
    @Schema(description = "Código único de operación (UPC) de la transacción en estado PENDIENTE_CONCILIACION", example = "UPC83_A1B2C3")
    private String upc;

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = "^[0-9]{10,20}$", message = "El número de cuenta debe contener entre 10 y 20 dígitos numéricos")
    @Schema(description = "Número de cuenta bancaria asociada a la operación", example = "4028193847")
    private String numeroCuenta;
}

