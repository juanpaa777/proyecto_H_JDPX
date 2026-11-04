package com.proyecto.servicios.dto.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Criterios combinables para búsqueda dinámica y paginada de usuarios mediante RequestBody")
public class ConsultaUsuarioRequest {

    @Schema(description = "ID del usuario (opcional)", example = "1")
    private Long id;

    @Schema(description = "ID del cliente asociado (opcional)", example = "10")
    private Long clienteId;

    @Schema(description = "Correo del usuario (búsqueda parcial o exacta)", example = "usuario@banco.com")
    private String correo;

    @Schema(description = "Estatus activo (true/false) (opcional)", example = "true")
    private Boolean activo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Fecha inicial del rango de registro", example = "2026-01-01T00:00:00")
    private LocalDateTime fechaInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Fecha final del rango de registro", example = "2026-12-31T23:59:59")
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
