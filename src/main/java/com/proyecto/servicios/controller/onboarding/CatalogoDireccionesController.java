package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.dto.onboarding.CodigoPostalRequest;
import com.proyecto.servicios.dto.onboarding.CodigoPostalResponse;
import com.proyecto.servicios.dto.onboarding.ColoniaRequest;
import com.proyecto.servicios.dto.onboarding.MunicipioRequest;
import com.proyecto.servicios.entity.onboarding.CatalogoSepomex;
import com.proyecto.servicios.service.onboarding.CatalogoDireccionesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalogos/domicilios")
@RequiredArgsConstructor
@Tag(name = "Catálogos de Domicilios", description = "Endpoints para consulta y sincronización de direcciones sin PathVariable ni RequestParam (100% RequestBody)")
public class CatalogoDireccionesController {

    private final CatalogoDireccionesService catalogoDireccionesService;

    @PostMapping("/cp")
    @Operation(summary = "Consultar información completa por Código Postal", description = "Devuelve el Estado, Municipio, Ciudad y lista de Colonias. Si no existe en la base de datos local, lo sincroniza automáticamente desde la API externa.")
    public ResponseEntity<CodigoPostalResponse> consultarPorCodigoPostal(@Valid @RequestBody CodigoPostalRequest request) {
        return ResponseEntity.ok(catalogoDireccionesService.consultarPorCodigoPostal(request.getCodigoPostal()));
    }

    @PostMapping("/sincronizar")
    @Operation(summary = "Sincronizar código postal desde la API externa a la Base de Datos", description = "Fuerza la petición a la API externa de Códigos Postales y persiste los registros en la base de datos local.")
    public ResponseEntity<List<CatalogoSepomex>> sincronizarCodigoPostal(@Valid @RequestBody CodigoPostalRequest request) {
        List<CatalogoSepomex> registros = catalogoDireccionesService.sincronizarDesdeApi(request.getCodigoPostal());
        return ResponseEntity.ok(registros);
    }

    @GetMapping("/estados")
    @Operation(summary = "Listar todos los Estados disponibles", description = "Devuelve la lista única de estados registrados en la base de datos.")
    public ResponseEntity<List<String>> obtenerEstados() {
        return ResponseEntity.ok(catalogoDireccionesService.obtenerEstados());
    }

    @PostMapping("/municipios")
    @Operation(summary = "Listar Municipios por Estado", description = "Devuelve los municipios pertenecientes al estado especificado en RequestBody.")
    public ResponseEntity<List<String>> obtenerMunicipios(@Valid @RequestBody MunicipioRequest request) {
        return ResponseEntity.ok(catalogoDireccionesService.obtenerMunicipiosPorEstado(request.getEstado()));
    }

    @PostMapping("/colonias")
    @Operation(summary = "Listar Colonias por Código Postal", description = "Devuelve el listado de colonias para un código postal enviado en RequestBody.")
    public ResponseEntity<List<String>> obtenerColonias(@Valid @RequestBody ColoniaRequest request) {
        return ResponseEntity.ok(catalogoDireccionesService.obtenerColoniasPorCodigoPostal(request.getCp()));
    }
}