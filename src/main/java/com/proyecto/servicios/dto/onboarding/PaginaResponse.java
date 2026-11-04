package com.proyecto.servicios.dto.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estructura estándar y unificada para respuestas paginadas de la API")
public class PaginaResponse<T> {

    @Schema(description = "Lista de elementos correspondientes a la página actual")
    private List<T> contenido;

    @Schema(description = "Número de página actual (índice base 0)", example = "0")
    private int paginaActual;

    @Schema(description = "Cantidad de elementos por página solicitados", example = "10")
    private int tamanoPagina;

    @Schema(description = "Total global de elementos disponibles", example = "45")
    private long totalElementos;

    @Schema(description = "Total global de páginas calculadas", example = "5")
    private int totalPaginas;

    @Schema(description = "Indica si es la primera página del catálogo", example = "true")
    private boolean esPrimera;

    @Schema(description = "Indica si es la última página del catálogo", example = "false")
    private boolean esUltima;

    @Schema(description = "Indica si existen páginas siguientes", example = "true")
    private boolean tieneSiguiente;

    @Schema(description = "Indica si existen páginas anteriores", example = "false")
    private boolean tieneAnterior;

    public static <T> PaginaResponse<T> from(Page<T> page) {
        return PaginaResponse.<T>builder()
                .contenido(page.getContent())
                .paginaActual(page.getNumber())
                .tamanoPagina(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .esPrimera(page.isFirst())
                .esUltima(page.isLast())
                .tieneSiguiente(page.hasNext())
                .tieneAnterior(page.hasPrevious())
                .build();
    }
}
