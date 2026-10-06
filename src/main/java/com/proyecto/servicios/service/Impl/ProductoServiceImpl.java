package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.model.gestopago.xml.MensajeXml;
import com.proyecto.servicios.model.gestopago.xml.ProductListXmlResponse;
import com.proyecto.servicios.model.gestopago.xml.ProductoXml;
import com.proyecto.servicios.model.producto.ProductoDto;
import com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository;
import com.proyecto.servicios.service.ProductoService;
import com.proyecto.servicios.service.XmlSerializationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    private final ProductoMongoRepository productoRepository;
    private final XmlSerializationService xmlSerializationService;

    public ProductoServiceImpl(
            ProductoMongoRepository productoRepository,
            XmlSerializationService xmlSerializationService
    ) {
        this.productoRepository = productoRepository;
        this.xmlSerializationService = xmlSerializationService;
    }

    @Override
    public List<ProductoDto> consultarCatalogo() {
        log.info("Consultando catálogo de productos activos desde MongoDB");
        List<ProductoDocument> productos = productoRepository.findAllByActivoTrue();
        log.info("Se encontraron {} productos activos en MongoDB", productos.size());
        return productos.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ProductoDto> consultarPorIdProducto(Integer idProducto) {
        log.info("Buscando producto con idProducto={} en MongoDB", idProducto);
        return productoRepository.findByIdProducto(idProducto)
                .map(this::toDto);
    }

    @Override
    public List<ProductoDto> consultarPorIdServicio(Integer idServicio) {
        log.info("Buscando productos para idServicio={} en MongoDB", idServicio);
        return productoRepository.findByIdServicioAndActivoTrue(idServicio)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public String exportarCatalogoXml() {
        log.info("Exportando catálogo de productos a formato XML serializado");
        List<ProductoDocument> productos = productoRepository.findAllByActivoTrue();

        List<ProductoXml> productosXml = productos.stream()
                .map(p -> ProductoXml.builder()
                        .idProducto(p.getIdProducto())
                        .idServicio(p.getIdServicio())
                        .servicio(p.getServicio())
                        .producto(p.getProducto())
                        .idCatTipoServicio(p.getIdCatTipoServicio())
                        .tipoFront(p.getTipoFront())
                        .hasDigitoVerificador(p.getHasDigitoVerificador())
                        .precio(p.getPrecio() != null ? p.getPrecio().toString() : "0.0")
                        .showAyuda(p.getShowAyuda())
                        .tipoReferencia(p.getTipoReferencia())
                        .legend(p.getLegend())
                        .build())
                .collect(Collectors.toList());

        ProductListXmlResponse responseXml = ProductListXmlResponse.builder()
                .mensaje(MensajeXml.builder()
                        .codigo("01")
                        .texto("Operacion realizada con exito")
                        .build())
                .productos(productosXml)
                .build();

        return xmlSerializationService.serializeToXml(responseXml);
    }

    private ProductoDto toDto(ProductoDocument doc) {
        return ProductoDto.builder()
                .id(doc.getId())
                .idProducto(doc.getIdProducto())
                .idServicio(doc.getIdServicio())
                .servicio(doc.getServicio())
                .producto(doc.getProducto())
                .idCatTipoServicio(doc.getIdCatTipoServicio())
                .tipoFront(doc.getTipoFront())
                .hasDigitoVerificador(doc.getHasDigitoVerificador())
                .precio(doc.getPrecio())
                .showAyuda(doc.getShowAyuda())
                .tipoReferencia(doc.getTipoReferencia())
                .legend(doc.getLegend())
                .activo(doc.getActivo())
                .fechaActualizacion(doc.getFechaActualizacion())
                .build();
    }
}
