package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.model.gestopago.xml.ProductListXmlResponse;
import com.proyecto.servicios.model.producto.ProductoDto;
import com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository;
import com.proyecto.servicios.service.Impl.ProductoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoMongoRepository productoRepository;

    @Mock
    private XmlSerializationService xmlSerializationService;

    private ProductoServiceImpl productoService;

    @BeforeEach
    void setUp() {
        productoService = new ProductoServiceImpl(productoRepository, xmlSerializationService);
    }

    @Test
    @DisplayName("Debe consultar y mapear los productos activos desde MongoDB a DTOs JSON")
    void testConsultarCatalogo() {
        ProductoDocument doc = ProductoDocument.builder()
                .id("mongo-id-1")
                .idProducto(185)
                .idServicio(56)
                .servicio("AGUAKAN")
                .producto("Agua Cancun")
                .precio(new BigDecimal("10.00"))
                .tipoFront(2)
                .tipoReferencia("c")
                .activo(true)
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(productoRepository.findAllByActivoTrue()).thenReturn(List.of(doc));

        List<ProductoDto> resultado = productoService.consultarCatalogo();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());

        ProductoDto dto = resultado.get(0);
        assertEquals(185, dto.getIdProducto());
        assertEquals(56, dto.getIdServicio());
        assertEquals("AGUAKAN", dto.getServicio());
        assertEquals(new BigDecimal("10.00"), dto.getPrecio());
        assertTrue(dto.getActivo());
    }

    @Test
    @DisplayName("Debe buscar un producto por su idProducto")
    void testConsultarPorIdProducto() {
        ProductoDocument doc = ProductoDocument.builder()
                .idProducto(200)
                .idServicio(71)
                .servicio("Amazon")
                .producto("Amazon $100")
                .precio(new BigDecimal("100.00"))
                .activo(true)
                .build();

        when(productoRepository.findByIdProducto(200)).thenReturn(Optional.of(doc));

        Optional<ProductoDto> resultado = productoService.consultarPorIdProducto(200);

        assertTrue(resultado.isPresent());
        assertEquals("Amazon $100", resultado.get().getProducto());
    }

    @Test
    @DisplayName("Debe exportar el catálogo completo serializado a XML")
    void testExportarCatalogoXml() {
        ProductoDocument doc = ProductoDocument.builder()
                .idProducto(500)
                .servicio("CFE")
                .producto("Pago de Luz")
                .precio(new BigDecimal("0.00"))
                .activo(true)
                .build();

        when(productoRepository.findAllByActivoTrue()).thenReturn(List.of(doc));
        when(xmlSerializationService.serializeToXml(any(ProductListXmlResponse.class)))
                .thenReturn("<RESPONSE><PRODUCTOS><producto servicio='CFE'/></PRODUCTOS></RESPONSE>");

        String xmlResultado = productoService.exportarCatalogoXml();

        assertNotNull(xmlResultado);
        assertTrue(xmlResultado.contains("CFE"));
        verify(xmlSerializationService).serializeToXml(any(ProductListXmlResponse.class));
    }
}
