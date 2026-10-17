package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.mongo.CatalogoSyncLogDocument;
import com.proyecto.servicios.model.producto.ProductoDto;
import com.proyecto.servicios.model.producto.ProductoIdRequest;
import com.proyecto.servicios.model.producto.ServicioIdRequest;
import com.proyecto.servicios.service.CatalogoSyncService;
import com.proyecto.servicios.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final CatalogoSyncService catalogoSyncService;

    public ProductoController(
            ProductoService productoService,
            CatalogoSyncService catalogoSyncService
    ) {
        this.productoService = productoService;
        this.catalogoSyncService = catalogoSyncService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProductoDto>> consultarCatalogo() {
        List<ProductoDto> catalogo = productoService.consultarCatalogo();
        return ResponseEntity.ok(catalogo);
    }

    @PostMapping(value = "/detalle", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductoDto> consultarPorId(@Valid @RequestBody ProductoIdRequest request) {
        return productoService.consultarPorIdProducto(request.getIdProducto())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping(value = "/servicio", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProductoDto>> consultarPorServicio(@Valid @RequestBody ServicioIdRequest request) {
        List<ProductoDto> productos = productoService.consultarPorIdServicio(request.getIdServicio());
        return ResponseEntity.ok(productos);
    }

    @PostMapping(value = "/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CatalogoSyncLogDocument> sincronizarManual() {
        CatalogoSyncLogDocument resultado = catalogoSyncService.sincronizarCatalogoManual();
        return "EXITO".equals(resultado.getEstado())
                ? ResponseEntity.ok(resultado)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(resultado);
    }

    @GetMapping(value = "/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> exportarXml() {
        String xml = productoService.exportarCatalogoXml();
        return ResponseEntity.ok(xml);
    }
}
