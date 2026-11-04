package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.CatalogoSyncLogDocument;
import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.model.gestopago.xml.ProductListXmlResponse;
import com.proyecto.servicios.model.gestopago.xml.ProductoXml;
import com.proyecto.servicios.repositorys.mongo.CatalogoSyncLogRepository;
import com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository;
import com.proyecto.servicios.service.CatalogoSyncService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.XmlSerializationService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CatalogoSyncServiceImpl implements CatalogoSyncService {

    private final GestoPagoProductClient productClient;
    private final GestoPagoTokenService tokenService;
    private final XmlSerializationService xmlSerializationService;
    private final ProductoMongoRepository productoRepository;
    private final CatalogoSyncLogRepository syncLogRepository;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    public CatalogoSyncServiceImpl(
            GestoPagoProductClient productClient,
            GestoPagoTokenService tokenService,
            XmlSerializationService xmlSerializationService,
            ProductoMongoRepository productoRepository,
            CatalogoSyncLogRepository syncLogRepository
    ) {
        this.productClient = productClient;
        this.tokenService = tokenService;
        this.xmlSerializationService = xmlSerializationService;
        this.productoRepository = productoRepository;
        this.syncLogRepository = syncLogRepository;
    }

    @Override
    @Scheduled(cron = "${gestopago.products.cron:0 0 6 * * ?}")
    public void sincronizarCatalogoProgramado() {
        log.info("⏰ [CRON 06:00 AM] Iniciando sincronización programada del catálogo de productos");
        ejecutarSincronizacion();
    }

    @Override
    public CatalogoSyncLogDocument sincronizarCatalogoManual() {
        log.info("Iniciando sincronización manual del catálogo de productos bajo demanda");
        return ejecutarSincronizacion();
    }

    private CatalogoSyncLogDocument ejecutarSincronizacion() {
        long inicioMs = System.currentTimeMillis();
        LocalDateTime fechaEjecucion = LocalDateTime.now();

        CatalogoSyncLogDocument logDoc = CatalogoSyncLogDocument.builder()
                .fechaEjecucion(fechaEjecucion)
                .estado("INICIADO")
                .totalProductos(0)
                .build();

        try {
            // 1. Obtener Bearer Token activo desde GestoPagoTokenService
            String token = obtenerTokenValido();
            if (token == null || token.trim().isEmpty()) {
                String errorMsg = "No fue posible obtener un Bearer Token válido para la autenticación en GestoPago";
                log.error("Error de autenticación: {}", errorMsg);
                return registrarFallo(logDoc, "ERROR_AUTENTICACION", 401, errorMsg, inicioMs);
            }

            String authHeader = "Bearer " + token;
            log.info("Invocando endpoint externo de GestoPago: GET /sistema/service/getProductList.do");

            // 2. Consumir endpoint del servicio externo
            String xmlResponse = productClient.getProductList(authHeader);
            logDoc.setXmlRaw(xmlResponse);

            if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
                String errorMsg = "La respuesta XML del servicio externo está vacía";
                log.warn(errorMsg);
                return registrarFallo(logDoc, "RESPUESTA_VACIA", 204, errorMsg, inicioMs);
            }

            // 3. Deserializar XML a modelos de datos Java mediante JAXB
            ProductListXmlResponse parsedResponse = xmlSerializationService.deserializeFromXml(
                    xmlResponse, ProductListXmlResponse.class
            );

            if (parsedResponse == null || parsedResponse.getMensaje() == null) {
                String errorMsg = "Estructura XML inválida o no compatible recibida de GestoPago";
                log.error(errorMsg);
                return registrarFallo(logDoc, "XML_INVALIDO", 500, errorMsg, inicioMs);
            }

            String codigoRespuesta = parsedResponse.getMensaje().getCodigo();
            String textoMensaje = parsedResponse.getMensaje().getTexto();
            logDoc.setCodigoRespuesta(parseCodigo(codigoRespuesta));
            logDoc.setMensajeRespuesta(textoMensaje);

            if (!"01".equals(codigoRespuesta)) {
                String errorMsg = String.format("GestoPago retornó código de respuesta no exitoso [codigo=%s, mensaje=%s]",
                        codigoRespuesta, textoMensaje);
                log.error(errorMsg);
                return registrarFallo(logDoc, "ERROR_RESPUESTA_EXTERNA", parseCodigo(codigoRespuesta), errorMsg, inicioMs);
            }

            // 4. Transformar y persistir productos en MongoDB
            List<ProductoXml> productosXml = parsedResponse.getProductos();
            int totalProcesados = guardarProductosEnMongo(productosXml);

            long duracionMs = System.currentTimeMillis() - inicioMs;
            logDoc.setEstado("EXITO");
            logDoc.setTotalProductos(totalProcesados);
            logDoc.setTiempoEjecucionMs(duracionMs);

            syncLogRepository.save(logDoc);
            log.info("Sincronización de catálogo finalizada con éxito: {} productos actualizados en MongoDB en {} ms",
                    totalProcesados, duracionMs);

            return logDoc;

        } catch (RetryableException e) {
            String errorMsg = "Timeout o fallo de conexión con el servicio externo de GestoPago: " + e.getMessage();
            log.error(errorMsg);
            return registrarFallo(logDoc, "TIMEOUT", 504, errorMsg, inicioMs);

        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            String errorMsg = "Error de autenticación al invocar GestoPago (HTTP " + e.status() + ")";
            log.error(errorMsg);
            return registrarFallo(logDoc, "ERROR_AUTENTICACION", e.status(), errorMsg, inicioMs);

        } catch (FeignException e) {
            String errorMsg = String.format("Error HTTP del servicio externo [status=%d]: %s", e.status(), e.getMessage());
            log.error(errorMsg);
            return registrarFallo(logDoc, "ERROR_HTTP_EXTERNO", e.status(), errorMsg, inicioMs);

        } catch (Exception e) {
            String errorMsg = "Error inesperado durante la sincronización del catálogo: " + e.getMessage();
            log.error(errorMsg, e);
            return registrarFallo(logDoc, "ERROR_INTERNO", 500, errorMsg, inicioMs);
        }
    }

    private int guardarProductosEnMongo(List<ProductoXml> productosXml) {
        if (productosXml == null || productosXml.isEmpty()) {
            log.warn("No se encontraron productos en la respuesta para guardar en MongoDB");
            return 0;
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<ProductoDocument> documentosAGuardar = new ArrayList<>();

        for (ProductoXml item : productosXml) {
            if (item.getIdProducto() == null) {
                continue;
            }

            ProductoDocument doc = productoRepository.findByIdProducto(item.getIdProducto())
                    .orElseGet(() -> ProductoDocument.builder()
                            .idProducto(item.getIdProducto())
                            .fechaCreacion(ahora)
                            .build()
                    );

            doc.setIdServicio(item.getIdServicio());
            doc.setServicio(item.getServicio());
            doc.setProducto(item.getProducto());
            doc.setIdCatTipoServicio(item.getIdCatTipoServicio());
            doc.setTipoFront(item.getTipoFront());
            doc.setHasDigitoVerificador(item.getHasDigitoVerificador());
            doc.setPrecio(parsePrecio(item.getPrecio()));
            doc.setShowAyuda(item.getShowAyuda());
            doc.setTipoReferencia(item.getTipoReferencia());
            doc.setLegend(item.getLegend());
            doc.setActivo(true);
            doc.setFechaActualizacion(ahora);

            documentosAGuardar.add(doc);
        }

        productoRepository.saveAll(documentosAGuardar);
        return documentosAGuardar.size();
    }

    private String obtenerTokenValido() {
        Optional<GestoPagoToken> tokenOpt = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
        if (tokenOpt.isPresent() && tokenOpt.get().getToken() != null) {
            return tokenOpt.get().getToken();
        }

        // Si no hay token en BD, intentar renovarlo de inmediato
        log.info("No se encontró token activo en BD, intentando renovar token...");
        try {
            tokenService.renovarToken();
            tokenOpt = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
            return tokenOpt.map(GestoPagoToken::getToken).orElse(null);
        } catch (Exception e) {
            log.error("Error al renovar token para sincronización: {}", e.getMessage());
            return null;
        }
    }

    private CatalogoSyncLogDocument registrarFallo(
            CatalogoSyncLogDocument logDoc, String estado, Integer codigo, String errorDetalle, long inicioMs) {
        logDoc.setEstado(estado);
        logDoc.setCodigoRespuesta(codigo);
        logDoc.setErrorDetalle(errorDetalle);
        logDoc.setTiempoEjecucionMs(System.currentTimeMillis() - inicioMs);
        try {
            syncLogRepository.save(logDoc);
        } catch (Exception ex) {
            log.error("No se pudo guardar el log de error en MongoDB: {}", ex.getMessage());
        }
        return logDoc;
    }

    private BigDecimal parsePrecio(String precioStr) {
        if (precioStr == null || precioStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(precioStr.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer parseCodigo(String codigoStr) {
        if (codigoStr == null) return null;
        try {
            return Integer.parseInt(codigoStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
