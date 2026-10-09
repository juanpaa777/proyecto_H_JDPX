package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.dto.onboarding.CodigoPostalResponse;
import com.proyecto.servicios.entity.onboarding.CatalogoSepomex;
import com.proyecto.servicios.service.onboarding.CatalogoDireccionesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalogos/domicilios")
@RequiredArgsConstructor
@Tag(name = "Catálogos de Domicilios", description = "Endpoints para consulta, autocompletado y sincronización de direcciones desde API externa hacia la Base de Datos")
public class CatalogoDireccionesController {

    private final CatalogoDireccionesService catalogoDireccionesService;

    @GetMapping("/cp/{codigoPostal}")
    @Operation(summary = "Consultar información completa por Código Postal", description = "Devuelve el Estado, Municipio, Ciudad y lista de Colonias. Si no existe en la base de datos local, lo sincroniza automáticamente desde la API externa.")
    public ResponseEntity<CodigoPostalResponse> consultarPorCodigoPostal(
            @Parameter(description = "Código postal de 5 dígitos", example = "37800")
            @PathVariable String codigoPostal) {
        return ResponseEntity.ok(catalogoDireccionesService.consultarPorCodigoPostal(codigoPostal));
    }

    @PostMapping("/sincronizar/{codigoPostal}")
    @Operation(summary = "Sincronizar código postal desde la API externa a la Base de Datos", description = "Fuerza la petición a la API externa de Códigos Postales y persiste los registros en la base de datos local.")
    public ResponseEntity<List<CatalogoSepomex>> sincronizarCodigoPostal(
            @Parameter(description = "Código postal de 5 dígitos", example = "37800")
            @PathVariable String codigoPostal) {
        List<CatalogoSepomex> registros = catalogoDireccionesService.sincronizarDesdeApi(codigoPostal);
        return ResponseEntity.ok(registros);
    }

    @GetMapping("/estados")
    @Operation(summary = "Listar todos los Estados disponibles", description = "Devuelve la lista única de estados registrados en la base de datos.")
    public ResponseEntity<List<String>> obtenerEstados() {
        return ResponseEntity.ok(catalogoDireccionesService.obtenerEstados());
    }

    @GetMapping("/municipios")
    @Operation(summary = "Listar Municipios por Estado", description = "Devuelve los municipios pertenecientes al estado especificado.")
    public ResponseEntity<List<String>> obtenerMunicipios(
            @Parameter(description = "Nombre del estado", example = "Guanajuato")
            @RequestParam String estado) {
        return ResponseEntity.ok(catalogoDireccionesService.obtenerMunicipiosPorEstado(estado));
    }

    @GetMapping("/colonias")
    @Operation(summary = "Listar Colonias por Código Postal", description = "Devuelve únicamente el listado de nombres de colonias para un código postal.")
    public ResponseEntity<List<String>> obtenerColonias(
            @Parameter(description = "Código postal de 5 dígitos", example = "37800")
            @RequestParam String cp) {
        return ResponseEntity.ok(catalogoDireccionesService.obtenerColoniasPorCodigoPostal(cp));
    }
}