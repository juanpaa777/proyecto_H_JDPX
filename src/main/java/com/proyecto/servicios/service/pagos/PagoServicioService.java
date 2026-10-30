package com.proyecto.servicios.service.pagos;

import com.google.gson.Gson;
import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.dto.pagos.ConciliarPagoRequest;
import com.proyecto.servicios.dto.pagos.ConsultaPagoServicioRequest;
import com.proyecto.servicios.dto.pagos.PagoServicioRequest;
import com.proyecto.servicios.dto.pagos.PagoServicioResponse;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.entity.pagos.PagoServicio;
import com.proyecto.servicios.exception.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.exception.onboarding.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.onboarding.UsuarioInactivoException;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.exception.pagos.*;
import com.proyecto.servicios.model.gestopago.xml.MensajeXml;
import com.proyecto.servicios.model.gestopago.xml.SendTxXmlResponse;
import com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.pagos.PagoServicioRepository;
import com.proyecto.servicios.repositorys.pagos.specification.PagoServicioSpecification;
import com.proyecto.servicios.security.pagos.GestoPagoAesService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.XmlSerializationService;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoServicioService {

    private static final DateTimeFormatter HORA_LOCAL_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final List<String> ESTATUS_BLOQUEANTES_DUPLICIDAD = List.of("PROCESANDO", "EXITOSA", "PENDIENTE_CONCILIACION");
    private static final BigDecimal MONTO_MINIMO_VARIABLE = new BigDecimal("20.00");
    private static final BigDecimal MONTO_MAXIMO_VARIABLE = new BigDecimal("10000.00");

    private final CuentaRepository cuentaRepository;
    private final ProductoMongoRepository productoMongoRepository;
    private final PagoServicioRepository pagoServicioRepository;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final GestoPagoAesService gestoPagoAesService;
    private final GestoPagoTxClient gestoPagoTxClient;
    private final XmlSerializationService xmlSerializationService;
    private final PasswordEncoder passwordEncoder;
    private final Gson gson = new Gson();

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidorDefault;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivoDefault;

    @Value("${gestopago.tx.unidad:083}")
    private String unidadDefault;

    @Value("${gestopago.tx.api-key:YSX1HpAFum4TpCecyFBxs4eIjAlbhKqK6fpcSQp8}")
    private String apiKey;

    /**
     * Ejecuta el flujo completo de pago de servicio o recarga:
     * 1. Bloqueo pesimista de cuenta, validación de cuenta activa, cliente activo y PIN/contraseña BCrypt.
     * 2. Validación de servicio, producto y precio en el catálogo de MongoDB.
     * 3. Validación de saldo suficiente y no duplicidad (UPC y ventana temporal).
     * 4. Construcción del payload según modalidad (con/sin referencia), firma AES-256 y random string.
     * 5. Obtención de Bearer Token desde BD e invocación a sendTx.do.
     * 6. Descuento del saldo únicamente si GestoPago retorna código "01".
     */
    @Transactional(
            rollbackFor = Exception.class,
            noRollbackFor = {
                    PagoRechazadoProveedorException.class,
                    ProveedorExternoTimeoutException.class,
                    TransaccionDuplicadaException.class
            }
    )
    public PagoServicioResponse procesarPagoServicio(PagoServicioRequest request) {
        // 1. Obtener Cuenta con bloqueo pesimista (control de concurrencia) y validar estatus activo
        Cuenta cuenta = cuentaRepository.findByNumeroCuentaForUpdate(request.getNumeroCuenta().trim())
                .orElseThrow(() -> new CuentaNoEncontradaException(request.getNumeroCuenta()));

        Optional.of(cuenta)
                .filter(c -> !"ACTIVA".equalsIgnoreCase(c.getEstatus()))
                .ifPresent(c -> { throw new ValidacionNegocioException("La cuenta bancaria no se encuentra activa para realizar operaciones"); });

        Cliente cliente = Optional.ofNullable(cuenta.getCliente())
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(() -> new UsuarioInactivoException("El cliente titular de la cuenta se encuentra inactivo"));

        // 2. Validar Usuario activo y contraseña/PIN seguro con BCrypt
        Usuario usuario = Optional.ofNullable(cliente.getUsuario())
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new UsuarioInactivoException("El usuario asociado a la cuenta se encuentra inactivo"));

        Optional.of(request.getPin())
                .filter(pin -> !passwordEncoder.matches(pin, usuario.getPassword()))
                .ifPresent(pin -> {
                    log.warn("Autorización rechazada para pago en cuenta {}: PIN/contraseña incorrecta", cuenta.getNumeroCuenta());
                    throw new CredencialesInvalidasException("Credenciales de autorización inválidas. Verifique su contraseña o PIN.");
                });

        // 3. Validar Producto, Servicio y Monto contra el catálogo de MongoDB
        ProductoDocument producto = productoMongoRepository.findByIdProducto(request.getIdProducto())
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> new ProductoNoValidoException("El producto con ID " + request.getIdProducto() + " no existe o está inactivo en el catálogo"));

        Optional.ofNullable(producto.getIdServicio())
                .filter(idServ -> !idServ.equals(request.getIdServicio()))
                .ifPresent(idServ -> {
                    throw new ProductoNoValidoException("El ID de servicio (" + request.getIdServicio() + ") no corresponde al producto solicitado (" + producto.getProducto() + ")");
                });

        validarMontoContraCatalogo(request.getPrecio(), producto);

        // 4. Validar Saldo Suficiente en la cuenta del cliente
        BigDecimal montoPago = request.getPrecio();
        Optional.of(cuenta.getSaldo())
                .filter(saldo -> saldo.compareTo(montoPago) < 0)
                .ifPresent(saldo -> {
                    throw new SaldoInsuficienteException("Saldo insuficiente en la cuenta para realizar el pago por $" + montoPago);
                });

        // 5. Determinar modalidad (CON_REFERENCIA vs SIN_REFERENCIA), teléfono y referencia
        String banderaNormalizada = normalizarBandera(request.getBandera(), producto);
        String telefonoPayload = resolverTelefonoPayload(banderaNormalizada, request.getTelefonoReferencia(), producto);
        String referenciaPayload = resolverReferenciaPayload(banderaNormalizada, request.getTelefonoReferencia());

        // 6. Validar Idempotencia / Prevención de Duplicados (UPC y ventanas de 15m / 24h)
        String upc = resolverYValidarUpc(request.getUpc());
        validarNoDuplicidadTemporal(banderaNormalizada, producto.getIdServicio(), telefonoPayload, referenciaPayload, montoPago);

        // 7. Construir Payload JSON, firmar con AES-256-CBC y generar cadena aleatoria (RandomStringUtils)
        String horaLocal = LocalDateTime.now().format(HORA_LOCAL_FORMATTER);
        String unidad = resolverUnidad(request.getDistribuidor());
        String jsonPayload = construirJsonPayload(
                producto.getIdProducto(),
                producto.getIdServicio(),
                telefonoPayload,
                horaLocal,
                referenciaPayload,
                montoPago,
                upc,
                unidad,
                banderaNormalizada
        );

        String payloadSigned = gestoPagoAesService.cifrarPayload(jsonPayload);
        String randomParam = System.currentTimeMillis() + "_" + RandomStringUtils.randomAlphanumeric(6);

        PagoServicio registroPago = PagoServicio.builder()
                .cliente(cliente)
                .cuenta(cuenta)
                .numeroCuenta(cuenta.getNumeroCuenta())
                .upc(upc)
                .idServicio(producto.getIdServicio())
                .idProducto(producto.getIdProducto())
                .nombreServicio(producto.getServicio())
                .nombreProducto(producto.getProducto())
                .tipoFront(producto.getTipoFront())
                .bandera(banderaNormalizada)
                .telefono(telefonoPayload)
                .referencia(referenciaPayload)
                .montoPago(montoPago)
                .comision(BigDecimal.ZERO)
                .saldoAnterior(cuenta.getSaldo())
                .saldoPosterior(cuenta.getSaldo())
                .horaLocal(horaLocal)
                .unidad(unidad)
                .randomParam(randomParam)
                .payloadSigned(payloadSigned)
                .estatus("PROCESANDO")
                .build();

        PagoServicio registroGuardado = pagoServicioRepository.save(registroPago);

        // 8. Obtener Token Bearer desde BD e invocar sendTx.do
        String xmlResponse = ejecutarSendTxConManejoDeTimeout(registroGuardado, randomParam, payloadSigned);
        registroGuardado.setXmlResponse(xmlResponse);

        SendTxXmlResponse parsedResponse = Optional.ofNullable(
                        xmlSerializationService.deserializeFromXml(xmlResponse, SendTxXmlResponse.class))
                .filter(r -> r.getMensaje() != null && r.getMensaje().getCodigo() != null)
                .orElseThrow(() -> {
                    registroGuardado.setEstatus("RECHAZADA");
                    registroGuardado.setMensajeRespuesta("Respuesta XML inválida del proveedor externo");
                    pagoServicioRepository.save(registroGuardado);
                    return new PagoRechazadoProveedorException("ERR_XML", "El proveedor de servicios devolvió una respuesta con formato no válido");
                });

        return procesarRespuestaProveedor(cuenta, registroGuardado, parsedResponse);
    }

    /**
     * Concilia una transacción en estado PENDIENTE_CONCILIACION invocando confirmTx.do
     * con exactamente los mismos parámetros firmados enviados en sendTx.do.
     */
    @Transactional(
            rollbackFor = Exception.class,
            noRollbackFor = {PagoRechazadoProveedorException.class}
    )
    public PagoServicioResponse conciliarPagoServicio(ConciliarPagoRequest request) {
        PagoServicio pago = pagoServicioRepository.findByUpc(request.getUpc().trim())
                .filter(p -> p.getNumeroCuenta().equals(request.getNumeroCuenta().trim()))
                .orElseThrow(() -> new ValidacionNegocioException("No se encontró ninguna transacción con el UPC y número de cuenta especificados"));

        Optional.of(pago.getEstatus())
                .filter(est -> !"PENDIENTE_CONCILIACION".equalsIgnoreCase(est))
                .ifPresent(est -> {
                    throw new ValidacionNegocioException("Solo se pueden conciliar transacciones en estatus PENDIENTE_CONCILIACION. Estatus actual: " + est);
                });

        Cuenta cuenta = cuentaRepository.findByNumeroCuentaForUpdate(pago.getNumeroCuenta())
                .orElseThrow(() -> new CuentaNoEncontradaException(pago.getNumeroCuenta()));

        String token = obtenerTokenValido();
        String randomConciliacion = System.currentTimeMillis() + "_" + RandomStringUtils.randomAlphanumeric(6);
        String formBody = construirFormUrlEncoded(randomConciliacion, pago.getPayloadSigned());

        String xmlResponse = invocarConfirmTxConRenovacion(token, formBody);
        pago.setXmlResponse(xmlResponse);

        SendTxXmlResponse parsed = Optional.ofNullable(
                        xmlSerializationService.deserializeFromXml(xmlResponse, SendTxXmlResponse.class))
                .filter(r -> r.getMensaje() != null && r.getMensaje().getCodigo() != null)
                .orElseThrow(() -> new PagoRechazadoProveedorException("ERR_XML", "Respuesta de conciliación inválida del proveedor"));

        String codigo = parsed.getMensaje().getCodigo().trim();
        String texto = Optional.ofNullable(parsed.getMensaje().getTexto()).map(String::trim).orElse("Resultado de conciliación");

        // En confirmTx.do, código "06" (o "01") con numAutorizacion != "0" confirma que la transacción SÍ fue aplicada
        boolean fueAplicada = ("06".equals(codigo) || "01".equals(codigo))
                && Optional.ofNullable(parsed.getNumAutorizacion()).map(a -> !"0".equals(a.trim()) && !"-1".equals(a.trim())).orElse(false);

        Optional.of(fueAplicada)
                .filter(Boolean::booleanValue)
                .ifPresentOrElse(
                        aplicada -> aplicarDescuentoYConfirmar(cuenta, pago, parsed, "01", texto),
                        () -> {
                            pago.setEstatus("RECHAZADA");
                            pago.setCodigoRespuesta(codigo);
                            pago.setMensajeRespuesta(texto);
                            pagoServicioRepository.save(pago);
                            throw new PagoRechazadoProveedorException(codigo, "Conciliación completada: la transacción no fue aplicada por el proveedor (" + texto + "). Su saldo no fue afectado.");
                        }
                );

        return toResponse(pago);
    }

    @Transactional(readOnly = true)
    public Page<PagoServicioResponse> consultarPagosPaginados(ConsultaPagoServicioRequest request) {
        int page = Optional.ofNullable(request.getPage()).orElse(0);
        int size = Optional.ofNullable(request.getSize()).orElse(10);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        return pagoServicioRepository.findAll(PagoServicioSpecification.conFiltros(request), pageable)
                .map(this::toResponse);
    }

    private void validarMontoContraCatalogo(BigDecimal montoSolicitado, ProductoDocument producto) {
        BigDecimal precioCatalogo = Optional.ofNullable(producto.getPrecio()).orElse(BigDecimal.ZERO);
        boolean esProductoPrecioFijo = Optional.ofNullable(producto.getTipoFront())
                .map(tf -> tf == 1)
                .orElse(true) && precioCatalogo.compareTo(BigDecimal.ZERO) > 0;

        // Si el producto es de precio fijo (tipoFront == 1, ej. recargas y pines), exigir coincidencia exacta con el catálogo
        Optional.of(esProductoPrecioFijo)
                .filter(Boolean::booleanValue)
                .filter(fijo -> precioCatalogo.compareTo(montoSolicitado) != 0)
                .ifPresent(fijo -> {
                    throw new MontoInvalidoException("El monto solicitado ($" + montoSolicitado + ") no coincide con el precio oficial del producto en catálogo ($" + precioCatalogo + ")");
                });

        // Si el producto es de pago de servicio de monto variable (tipoFront != 1 o precio 0 en catálogo), validar límites permitidos
        BigDecimal minimoPermitido = precioCatalogo.compareTo(BigDecimal.ZERO) > 0 ? precioCatalogo : MONTO_MINIMO_VARIABLE;
        Optional.of(esProductoPrecioFijo)
                .filter(Predicate.not(Boolean::booleanValue))
                .filter(var -> montoSolicitado.compareTo(minimoPermitido) < 0 || montoSolicitado.compareTo(MONTO_MAXIMO_VARIABLE) > 0)
                .ifPresent(var -> {
                    throw new MontoInvalidoException("Para este servicio, el monto de pago debe estar entre $" + minimoPermitido + " y $" + MONTO_MAXIMO_VARIABLE);
                });
    }

    private String normalizarBandera(String banderaInput, ProductoDocument producto) {
        String limpia = banderaInput.trim().toUpperCase();
        boolean esConReferenciaPorBandera = "CON_REFERENCIA".equals(limpia) || "2".equals(limpia);
        boolean esConReferenciaPorTipoFront = Optional.ofNullable(producto.getTipoFront())
                .map(tf -> tf == 2 || tf == 4)
                .orElse(false);

        return Optional.of(esConReferenciaPorBandera || esConReferenciaPorTipoFront)
                .filter(Boolean::booleanValue)
                .map(b -> "CON_REFERENCIA")
                .orElse("SIN_REFERENCIA");
    }

    private String resolverTelefonoPayload(String banderaNormalizada, String telefonoReferencia, ProductoDocument producto) {
        boolean esPinEntretenimiento = Optional.ofNullable(producto.getIdCatTipoServicio())
                .map(cat -> cat == 10 || cat == 11)
                .orElse(false);

        return Optional.of(esPinEntretenimiento)
                .filter(Boolean::booleanValue)
                .map(b -> "#1111111111")
                .orElseGet(() -> Optional.of(banderaNormalizada)
                        .filter("CON_REFERENCIA"::equals)
                        .map(b -> "1111111111")
                        .orElseGet(() -> Optional.of(telefonoReferencia.trim())
                                .filter(tel -> tel.matches("^[0-9]{10}$"))
                                .orElseThrow(() -> new ValidacionNegocioException("Para pagos sin referencia (recargas), el teléfono debe contener exactamente 10 dígitos numéricos"))));
    }

    private String resolverReferenciaPayload(String banderaNormalizada, String telefonoReferencia) {
        return Optional.of(banderaNormalizada)
                .filter("CON_REFERENCIA"::equals)
                .map(b -> telefonoReferencia.trim())
                .orElseGet(() -> "REF_" + RandomStringUtils.randomAlphanumeric(8).toUpperCase());
    }

    private String resolverYValidarUpc(String upcInput) {
        return Optional.ofNullable(upcInput)
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .map(u -> {
                    Optional.of(u)
                            .filter(pagoServicioRepository::existsByUpc)
                            .ifPresent(dup -> {
                                throw new TransaccionDuplicadaException("Solicitud duplicada: el código UPC '" + dup + "' ya fue procesado anteriormente");
                            });
                    return u;
                })
                .orElseGet(this::generarUpcUnico);
    }

    private String generarUpcUnico() {
        String prefijo = "GP" + idDistribuidorDefault + "_";
        int longitudAleatoria = Math.max(4, Math.min(8, 15 - prefijo.length()));
        String candidato = prefijo + RandomStringUtils.randomAlphanumeric(longitudAleatoria).toUpperCase();
        return Optional.of(candidato)
                .filter(Predicate.not(pagoServicioRepository::existsByUpc))
                .orElseGet(() -> prefijo + RandomStringUtils.randomNumeric(longitudAleatoria));
    }

    private void validarNoDuplicidadTemporal(
            String banderaNormalizada,
            Integer idServicio,
            String telefono,
            String referencia,
            BigDecimal montoPago
    ) {
        Optional.of(banderaNormalizada)
                .filter("CON_REFERENCIA"::equals)
                .filter(b -> pagoServicioRepository.existePagoServicioReciente(
                        idServicio, referencia, montoPago, ESTATUS_BLOQUEANTES_DUPLICIDAD, LocalDateTime.now().minusHours(24)))
                .ifPresent(b -> {
                    throw new TransaccionDuplicadaException("Operación duplicada: ya existe un pago para este servicio, referencia y monto en las últimas 24 horas");
                });

        Optional.of(banderaNormalizada)
                .filter("SIN_REFERENCIA"::equals)
                .filter(b -> !"#1111111111".equals(telefono))
                .filter(b -> pagoServicioRepository.existeRecargaReciente(
                        idServicio, telefono, ESTATUS_BLOQUEANTES_DUPLICIDAD, LocalDateTime.now().minusMinutes(15)))
                .ifPresent(b -> {
                    throw new TransaccionDuplicadaException("Operación duplicada: ya se realizó una recarga al mismo número y servicio en los últimos 15 minutos");
                });
    }

    private String resolverUnidad(Integer distribuidorRequest) {
        return Optional.ofNullable(distribuidorRequest)
                .map(d -> String.format("%03d", d))
                .orElse(unidadDefault);
    }

    private String construirJsonPayload(
            Integer idProducto,
            Integer idServicio,
            String telefono,
            String horaLocal,
            String referencia,
            BigDecimal montoPago,
            String upc,
            String unidad,
            String banderaNormalizada
    ) {
        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("idProducto", idProducto);
        payloadMap.put("idServicio", idServicio);
        payloadMap.put("telefono", telefono);
        payloadMap.put("horaLocal", horaLocal);

        Optional.of(banderaNormalizada)
                .filter("CON_REFERENCIA"::equals)
                .ifPresent(b -> {
                    payloadMap.put("referencia", referencia);
                    payloadMap.put("montoPago", montoPago);
                });

        payloadMap.put("upc", upc);
        payloadMap.put("unidad", unidad);

        return gson.toJson(payloadMap);
    }

    private String construirFormUrlEncoded(String randomParam, String payloadSigned) {
        return "random=" + URLEncoder.encode(randomParam, StandardCharsets.UTF_8)
                + "&signed=" + URLEncoder.encode(payloadSigned, StandardCharsets.UTF_8);
    }

    private String ejecutarSendTxConManejoDeTimeout(PagoServicio registroPago, String randomParam, String payloadSigned) {
        String token = obtenerTokenValido();
        String formBody = construirFormUrlEncoded(randomParam, payloadSigned);

        try {
            return invocarSendTxConRenovacion(token, formBody);
        } catch (RetryableException e) {
            log.error("Timeout de red al invocar sendTx.do para UPC {}. Marcando como PENDIENTE_CONCILIACION", registroPago.getUpc());
            registroPago.setEstatus("PENDIENTE_CONCILIACION");
            registroPago.setCodigoRespuesta("82");
            registroPago.setMensajeRespuesta("Tiempo de espera agotado con el proveedor. Requiere conciliación mediante confirmTx.");
            pagoServicioRepository.save(registroPago);
            throw new ProveedorExternoTimeoutException(
                    registroPago.getUpc(),
                    "Tiempo de espera agotado al comunicar con el proveedor de servicios. No reintente el pago directamente; utilice el servicio de conciliación con el UPC: " + registroPago.getUpc()
            );
        } catch (FeignException e) {
            log.error("Error HTTP {} al comunicar con proveedor externo para UPC {}", e.status(), registroPago.getUpc());
            registroPago.setEstatus("RECHAZADA");
            registroPago.setCodigoRespuesta("HTTP_" + e.status());
            registroPago.setMensajeRespuesta("Error de comunicación con el proveedor externo");
            pagoServicioRepository.save(registroPago);
            throw new PagoRechazadoProveedorException("HTTP_" + e.status(), "No fue posible procesar el pago con el proveedor externo en este momento.");
        }
    }

    private String invocarSendTxConRenovacion(String tokenInicial, String formBody) {
        try {
            return gestoPagoTxClient.sendTx("Bearer " + tokenInicial, apiKey, formBody);
        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            log.info("Token de GestoPago expirado (HTTP {}), renovando token automáticamente...", e.status());
            gestoPagoTokenService.renovarToken();
            String nuevoToken = obtenerTokenValido();
            return gestoPagoTxClient.sendTx("Bearer " + nuevoToken, apiKey, formBody);
        }
    }

    private String invocarConfirmTxConRenovacion(String tokenInicial, String formBody) {
        try {
            return gestoPagoTxClient.confirmTx("Bearer " + tokenInicial, apiKey, formBody);
        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            log.info("Token de GestoPago expirado en confirmTx (HTTP {}), renovando token...", e.status());
            gestoPagoTokenService.renovarToken();
            String nuevoToken = obtenerTokenValido();
            return gestoPagoTxClient.confirmTx("Bearer " + nuevoToken, apiKey, formBody);
        }
    }

    private String obtenerTokenValido() {
        return gestoPagoTokenService.obtenerTokenActivo(idDistribuidorDefault, codigoDispositivoDefault)
                .map(GestoPagoToken::getToken)
                .filter(Predicate.not(String::isBlank))
                .orElseGet(() -> {
                    gestoPagoTokenService.renovarToken();
                    return gestoPagoTokenService.obtenerTokenActivo(idDistribuidorDefault, codigoDispositivoDefault)
                            .map(GestoPagoToken::getToken)
                            .filter(Predicate.not(String::isBlank))
                            .orElseThrow(() -> new PagoRechazadoProveedorException("AUTH_ERR", "No fue posible autenticar la sesión segura con el proveedor de servicios"));
                });
    }

    private PagoServicioResponse procesarRespuestaProveedor(
            Cuenta cuenta,
            PagoServicio registroPago,
            SendTxXmlResponse parsedResponse
    ) {
        MensajeXml msg = parsedResponse.getMensaje();
        String codigo = msg.getCodigo().trim();
        String texto = Optional.ofNullable(msg.getTexto()).map(String::trim).orElse("Sin mensaje del proveedor");

        // Caso 1: Código "82" -> Timeout reportado por GestoPago (requiere confirmTx)
        Optional.of(codigo)
                .filter("82"::equals)
                .ifPresent(c -> {
                    registroPago.setEstatus("PENDIENTE_CONCILIACION");
                    registroPago.setCodigoRespuesta(c);
                    registroPago.setMensajeRespuesta(texto);
                    pagoServicioRepository.save(registroPago);
                    throw new ProveedorExternoTimeoutException(
                            registroPago.getUpc(),
                            "Tiempo de espera alcanzado en el proveedor (" + texto + "). La operación quedó en estado PENDIENTE_CONCILIACION con UPC: " + registroPago.getUpc()
                    );
                });

        // Caso 2: Código "06" -> Transacción duplicada reportada por GestoPago
        Optional.of(codigo)
                .filter("06"::equals)
                .ifPresent(c -> {
                    registroPago.setEstatus("RECHAZADA");
                    registroPago.setCodigoRespuesta(c);
                    registroPago.setMensajeRespuesta(texto);
                    pagoServicioRepository.save(registroPago);
                    throw new TransaccionDuplicadaException("El proveedor rechazó la operación por duplicidad: " + texto);
                });

        // Caso 3: Cualquier código distinto de "01" -> Rechazo sin descontar saldo
        Optional.of(codigo)
                .filter(c -> !"01".equals(c))
                .ifPresent(c -> {
                    registroPago.setEstatus("RECHAZADA");
                    registroPago.setCodigoRespuesta(c);
                    registroPago.setMensajeRespuesta(texto);
                    pagoServicioRepository.save(registroPago);
                    throw new PagoRechazadoProveedorException(c, "Pago rechazado por el proveedor [Código " + c + "]: " + texto);
                });

        // Caso 4: Código "01" -> ÉXITO: Descontar el saldo de la cuenta del cliente y confirmar transacción
        aplicarDescuentoYConfirmar(cuenta, registroPago, parsedResponse, codigo, texto);
        return toResponse(registroPago);
    }

    private void aplicarDescuentoYConfirmar(
            Cuenta cuenta,
            PagoServicio registroPago,
            SendTxXmlResponse parsedResponse,
            String codigo,
            String texto
    ) {
        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal nuevoSaldo = saldoAnterior.subtract(registroPago.getMontoPago());

        cuenta.setSaldo(nuevoSaldo);
        cuentaRepository.save(cuenta);

        MensajeXml msg = parsedResponse.getMensaje();
        String idTx = Optional.ofNullable(parsedResponse.getIdTx())
                .filter(Predicate.not(String::isBlank))
                .orElseGet(() -> Optional.ofNullable(msg.getIdTx()).orElse(null));

        registroPago.setSaldoAnterior(saldoAnterior);
        registroPago.setSaldoPosterior(nuevoSaldo);
        registroPago.setEstatus("EXITOSA");
        registroPago.setCodigoRespuesta(codigo);
        registroPago.setMensajeRespuesta(texto);
        registroPago.setIdTxGestopago(idTx);
        registroPago.setNumAutorizacion(parsedResponse.getNumAutorizacion());
        registroPago.setComision(parseBigDecimalSeguro(parsedResponse.getComision()));
        registroPago.setPin(Optional.ofNullable(msg.getPin()).map(String::trim).filter(Predicate.not(String::isEmpty)).orElse(null));
        registroPago.setLeyenda(Optional.ofNullable(msg.getLegend()).map(String::trim).filter(Predicate.not(String::isEmpty)).orElse(null));

        pagoServicioRepository.save(registroPago);
        log.info("Pago de servicio EXITOSO [Código 01]. UPC: {}, Cuenta: {}, Monto descontado: ${}, Nuevo saldo: ${}",
                registroPago.getUpc(), cuenta.getNumeroCuenta(), registroPago.getMontoPago(), nuevoSaldo);
    }

    private BigDecimal parseBigDecimalSeguro(String valor) {
        return Optional.ofNullable(valor)
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .map(v -> {
                    try {
                        return new BigDecimal(v);
                    } catch (NumberFormatException ex) {
                        return BigDecimal.ZERO;
                    }
                })
                .orElse(BigDecimal.ZERO);
    }

    private PagoServicioResponse toResponse(PagoServicio p) {
        return PagoServicioResponse.builder()
                .id(p.getId())
                .clienteId(Optional.ofNullable(p.getCliente()).map(Cliente::getId).orElse(null))
                .numeroCuenta(p.getNumeroCuenta())
                .upc(p.getUpc())
                .idServicio(p.getIdServicio())
                .nombreServicio(p.getNombreServicio())
                .idProducto(p.getIdProducto())
                .nombreProducto(p.getNombreProducto())
                .bandera(p.getBandera())
                .telefono(p.getTelefono())
                .referencia(p.getReferencia())
                .montoPago(p.getMontoPago())
                .comision(p.getComision())
                .saldoAnterior(p.getSaldoAnterior())
                .saldoPosterior(p.getSaldoPosterior())
                .estatus(p.getEstatus())
                .codigoRespuesta(p.getCodigoRespuesta())
                .mensajeRespuesta(p.getMensajeRespuesta())
                .idTxGestopago(p.getIdTxGestopago())
                .numAutorizacion(p.getNumAutorizacion())
                .pin(p.getPin())
                .leyenda(p.getLeyenda())
                .fechaCreacion(p.getFechaCreacion())
                .fechaActualizacion(p.getFechaActualizacion())
                .build();
    }
}
