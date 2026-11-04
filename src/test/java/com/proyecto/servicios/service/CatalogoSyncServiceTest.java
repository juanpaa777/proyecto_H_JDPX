package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.CatalogoSyncLogDocument;
import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.model.gestopago.xml.MensajeXml;
import com.proyecto.servicios.model.gestopago.xml.ProductListXmlResponse;
import com.proyecto.servicios.model.gestopago.xml.ProductoXml;
import com.proyecto.servicios.repositorys.mongo.CatalogoSyncLogRepository;
import com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository;
import com.proyecto.servicios.service.Impl.CatalogoSyncServiceImpl;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoSyncServiceTest {

    @Mock
    private GestoPagoProductClient productClient;

    @Mock
    private GestoPagoTokenService tokenService;

    @Mock
    private XmlSerializationService xmlSerializationService;

    @Mock
    private ProductoMongoRepository productoRepository;

    @Mock
    private CatalogoSyncLogRepository syncLogRepository;

    private CatalogoSyncServiceImpl catalogoSyncService;

    @BeforeEach
    void setUp() {
        catalogoSyncService = new CatalogoSyncServiceImpl(
                productClient,
                tokenService,
                xmlSerializationService,
                productoRepository,
                syncLogRepository
        );
    }

    @Test
    @DisplayName("Debe sincronizar y guardar productos en MongoDB cuando la respuesta es exitosa (CODIGO=01)")
    void testSincronizarCatalogoExitoso() {
        String tokenString = "mock_jwt_token_12345";
        GestoPagoToken tokenEntity = new GestoPagoToken();
        tokenEntity.setToken(tokenString);

        when(tokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.of(tokenEntity));

        String rawXml = "<RESPONSE><MENSAJE><CODIGO>01</CODIGO><TEXTO>OK</TEXTO></MENSAJE></RESPONSE>";
        when(productClient.getProductList("Bearer " + tokenString)).thenReturn(rawXml);

        ProductListXmlResponse parsedResponse = ProductListXmlResponse.builder()
                .mensaje(MensajeXml.builder().codigo("01").texto("Operacion realizada con exito").build())
                .productos(List.of(
                        ProductoXml.builder()
                                .idProducto(185)
                                .idServicio(56)
                                .servicio("AGUAKAN")
                                .producto("Agua Cancun")
                                .precio("10.0")
                                .tipoFront(2)
                                .tipoReferencia("c")
                                .legend("Instrucciones")
                                .build()
                ))
                .build();

        when(xmlSerializationService.deserializeFromXml(eq(rawXml), eq(ProductListXmlResponse.class)))
                .thenReturn(parsedResponse);
        when(productoRepository.findByIdProducto(185)).thenReturn(Optional.empty());

        CatalogoSyncLogDocument resultado = catalogoSyncService.sincronizarCatalogoManual();

        assertNotNull(resultado);
        assertEquals("EXITO", resultado.getEstado());
        assertEquals(1, resultado.getTotalProductos());

        verify(productoRepository).saveAll(any());
        verify(syncLogRepository).save(any(CatalogoSyncLogDocument.class));
    }

    @Test
    @DisplayName("Debe registrar fallo cuando GestoPago responde con código distinto de 01")
    void testSincronizarCatalogoCodigoError() {
        GestoPagoToken tokenEntity = new GestoPagoToken();
        tokenEntity.setToken("mock_token");
        when(tokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.of(tokenEntity));

        String rawXml = "<RESPONSE><MENSAJE><CODIGO>02</CODIGO><TEXTO>Error en servicio</TEXTO></MENSAJE></RESPONSE>";
        when(productClient.getProductList(anyString())).thenReturn(rawXml);

        ProductListXmlResponse parsedResponse = ProductListXmlResponse.builder()
                .mensaje(MensajeXml.builder().codigo("02").texto("Error en servicio").build())
                .productos(Collections.emptyList())
                .build();

        when(xmlSerializationService.deserializeFromXml(eq(rawXml), eq(ProductListXmlResponse.class)))
                .thenReturn(parsedResponse);

        CatalogoSyncLogDocument resultado = catalogoSyncService.sincronizarCatalogoManual();

        assertEquals("ERROR_RESPUESTA_EXTERNA", resultado.getEstado());
        assertEquals(2, resultado.getCodigoRespuesta());
        verify(productoRepository, never()).saveAll(any());
        verify(syncLogRepository).save(any(CatalogoSyncLogDocument.class));
    }

    @Test
    @DisplayName("Debe capturar Timeout (RetryableException) y registrar estado TIMEOUT en log")
    void testSincronizarCatalogoTimeout() {
        GestoPagoToken tokenEntity = new GestoPagoToken();
        tokenEntity.setToken("mock_token");
        when(tokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.of(tokenEntity));

        Request request = Request.create(Request.HttpMethod.GET, "http://gestopago", Collections.emptyMap(), null, new RequestTemplate());
        when(productClient.getProductList(anyString()))
                .thenThrow(new RetryableException(504, "Read timed out", Request.HttpMethod.GET, new Date(), request));

        CatalogoSyncLogDocument resultado = catalogoSyncService.sincronizarCatalogoManual();

        assertEquals("TIMEOUT", resultado.getEstado());
        assertEquals(504, resultado.getCodigoRespuesta());
        verify(productoRepository, never()).saveAll(any());
        verify(syncLogRepository).save(any(CatalogoSyncLogDocument.class));
    }

    @Test
    @DisplayName("Debe capturar error 401 Unauthorized y registrar ERROR_AUTENTICACION")
    void testSincronizarCatalogoUnauthorized() {
        GestoPagoToken tokenEntity = new GestoPagoToken();
        tokenEntity.setToken("mock_token");
        when(tokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.of(tokenEntity));

        Request request = Request.create(Request.HttpMethod.GET, "http://gestopago", Collections.emptyMap(), null, new RequestTemplate());
        when(productClient.getProductList(anyString()))
                .thenThrow(new FeignException.Unauthorized("Token expired", request, null, Collections.emptyMap()));

        CatalogoSyncLogDocument resultado = catalogoSyncService.sincronizarCatalogoManual();

        assertEquals("ERROR_AUTENTICACION", resultado.getEstado());
        assertEquals(401, resultado.getCodigoRespuesta());
        verify(syncLogRepository).save(any(CatalogoSyncLogDocument.class));
    }
}
