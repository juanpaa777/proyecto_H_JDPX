package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.mongo.CatalogoSyncLogDocument;
import com.proyecto.servicios.model.producto.ProductoDto;
import com.proyecto.servicios.service.CatalogoSyncService;
import com.proyecto.servicios.service.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping(value = "/{idProducto}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductoDto> consultarPorId(@PathVariable Integer idProducto) {
        return productoService.consultarPorIdProducto(idProducto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping(value = "/servicio/{idServicio}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProductoDto>> consultarPorServicio(@PathVariable Integer idServicio) {
        List<ProductoDto> productos = productoService.consultarPorIdServicio(idServicio);
        return ResponseEntity.ok(productos);
    }

    @PostMapping(value = "/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CatalogoSyncLogDocument> sincronizarManual() {
        CatalogoSyncLogDocument resultado = catalogoSyncService.sincronizarCatalogoManual();
        if ("EXITO".equals(resultado.getEstado())) {
            return ResponseEntity.ok(resultado);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(resultado);
        }
    }

    @GetMapping(value = "/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> exportarXml() {
        String xml = productoService.exportarCatalogoXml();
        return ResponseEntity.ok(xml);
    }
}
