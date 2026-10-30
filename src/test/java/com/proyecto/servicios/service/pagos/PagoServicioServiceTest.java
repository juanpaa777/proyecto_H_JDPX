package com.proyecto.servicios.service.pagos;

import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.dto.pagos.ConciliarPagoRequest;
import com.proyecto.servicios.dto.pagos.PagoServicioRequest;
import com.proyecto.servicios.dto.pagos.PagoServicioResponse;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.entity.pagos.PagoServicio;
import com.proyecto.servicios.exception.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.exception.pagos.*;
import com.proyecto.servicios.model.gestopago.xml.MensajeXml;
import com.proyecto.servicios.model.gestopago.xml.SendTxXmlResponse;
import com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.pagos.PagoServicioRepository;
import com.proyecto.servicios.security.pagos.GestoPagoAesService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.XmlSerializationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServicioServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private ProductoMongoRepository productoMongoRepository;
    @Mock
    private PagoServicioRepository pagoServicioRepository;
    @Mock
    private GestoPagoTokenService gestoPagoTokenService;
    @Mock
    private GestoPagoAesService gestoPagoAesService;
    @Mock
    private GestoPagoTxClient gestoPagoTxClient;
    @Mock
    private XmlSerializationService xmlSerializationService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PagoServicioService pagoServicioService;

    private Cliente clienteActivo;
    private Usuario usuarioActivo;
    private Cuenta cuentaActiva;
    private ProductoDocument productoServicio;
    private GestoPagoToken tokenActivo;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pagoServicioService, "idDistribuidorDefault", 83);
        ReflectionTestUtils.setField(pagoServicioService, "codigoDispositivoDefault", "GPS83-TPV-17");
        ReflectionTestUtils.setField(pagoServicioService, "unidadDefault", "083");
        ReflectionTestUtils.setField(pagoServicioService, "apiKey", "TEST_API_KEY");

        clienteActivo = new Cliente();
        clienteActivo.setId(1L);
        clienteActivo.setNombre("Diego");
        clienteActivo.setActivo(true);

        usuarioActivo = new Usuario();
        usuarioActivo.setId(1L);
        usuarioActivo.setCliente(clienteActivo);
        usuarioActivo.setCorreo("diego@banco.com");
        usuarioActivo.setPassword("$2a$12$hashedPassword");
        usuarioActivo.setActivo(true);
        clienteActivo.setUsuario(usuarioActivo);

        cuentaActiva = new Cuenta();
        cuentaActiva.setId(10L);
        cuentaActiva.setCliente(clienteActivo);
        cuentaActiva.setNumeroCuenta("4028193847");
        cuentaActiva.setSaldo(new BigDecimal("1000.00"));
        cuentaActiva.setEstatus("ACTIVA");

        productoServicio = ProductoDocument.builder()
                .idProducto(205)
                .idServicio(76)
                .servicio("CFE")
                .producto("Pago CFE")
                .tipoFront(2)
                .idCatTipoServicio(2)
                .precio(BigDecimal.ZERO) // Monto variable
                .activo(true)
                .build();

        tokenActivo = new GestoPagoToken();
        tokenActivo.setToken("jwt-token-seguro");
        tokenActivo.setActivo(true);
    }

    @Test
    @DisplayName("Código 01 en sendTx.do: debe descontar el saldo de la cuenta y registrar el pago como EXITOSA")
    void debeDescontarSaldoCuandoCodigoEs01() {
        PagoServicioRequest request = PagoServicioRequest.builder()
                .numeroCuenta("4028193847")
                .idServicio(76)
                .idProducto(205)
                .precio(new BigDecimal("350.00"))
                .telefonoReferencia("9731204028806")
                .bandera("CON_REFERENCIA")
                .pin("Password123!")
                .upc("GP83_UNICO01")
                .build();

        String xmlExito = "<RESPONSE><ID_TX>291620941</ID_TX><NUM_AUTORIZACION>100074211</NUM_AUTORIZACION>" +
                "<COMISION>6.0</COMISION><MENSAJE><CODIGO>01</CODIGO><TEXTO>Operacion realizada con exito</TEXTO></MENSAJE></RESPONSE>";

        SendTxXmlResponse parsed = SendTxXmlResponse.builder()
                .idTx("291620941")
                .numAutorizacion("100074211")
                .comision("6.0")
                .mensaje(MensajeXml.builder().codigo("01").texto("Operacion realizada con exito").build())
                .build();

        when(cuentaRepository.findByNumeroCuentaForUpdate("4028193847")).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("Password123!", "$2a$12$hashedPassword")).thenReturn(true);
        when(productoMongoRepository.findByIdProducto(205)).thenReturn(Optional.of(productoServicio));
        when(pagoServicioRepository.existsByUpc("GP83_UNICO01")).thenReturn(false);
        when(pagoServicioRepository.existePagoServicioReciente(anyInt(), anyString(), any(), anyList(), any())).thenReturn(false);
        when(gestoPagoAesService.cifrarPayload(anyString())).thenReturn("PAYLOAD_CIFRADO_AES");
        when(pagoServicioRepository.save(any(PagoServicio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenActivo));
        when(gestoPagoTxClient.sendTx(eq("Bearer jwt-token-seguro"), eq("TEST_API_KEY"), anyString())).thenReturn(xmlExito);
        when(xmlSerializationService.deserializeFromXml(xmlExito, SendTxXmlResponse.class)).thenReturn(parsed);

        PagoServicioResponse response = pagoServicioService.procesarPagoServicio(request);

        assertEquals("EXITOSA", response.getEstatus());
        assertEquals("01", response.getCodigoRespuesta());
        assertEquals(new BigDecimal("1000.00"), response.getSaldoAnterior());
        assertEquals(new BigDecimal("650.00"), response.getSaldoPosterior());
        assertEquals(new BigDecimal("650.00"), cuentaActiva.getSaldo());
        verify(cuentaRepository, times(1)).save(cuentaActiva);
    }

    @Test
    @DisplayName("Código distinto de 01 en sendTx.do: NO debe descontar el saldo de la cuenta y debe lanzar PagoRechazadoProveedorException")
    void noDebeDescontarSaldoCuandoCodigoNoEs01() {
        PagoServicioRequest request = PagoServicioRequest.builder()
                .numeroCuenta("4028193847")
                .idServicio(76)
                .idProducto(205)
                .precio(new BigDecimal("350.00"))
                .telefonoReferencia("9731204028806")
                .bandera("CON_REFERENCIA")
                .pin("Password123!")
                .upc("GP83_FAIL02")
                .build();

        String xmlError = "<RESPONSE><NUM_AUTORIZACION>-1</NUM_AUTORIZACION><MENSAJE><CODIGO>02</CODIGO><TEXTO>Referencia invalida</TEXTO></MENSAJE></RESPONSE>";
        SendTxXmlResponse parsed = SendTxXmlResponse.builder()
                .numAutorizacion("-1")
                .mensaje(MensajeXml.builder().codigo("02").texto("Referencia invalida").build())
                .build();

        when(cuentaRepository.findByNumeroCuentaForUpdate("4028193847")).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("Password123!", "$2a$12$hashedPassword")).thenReturn(true);
        when(productoMongoRepository.findByIdProducto(205)).thenReturn(Optional.of(productoServicio));
        when(pagoServicioRepository.existsByUpc("GP83_FAIL02")).thenReturn(false);
        when(pagoServicioRepository.existePagoServicioReciente(anyInt(), anyString(), any(), anyList(), any())).thenReturn(false);
        when(gestoPagoAesService.cifrarPayload(anyString())).thenReturn("PAYLOAD_CIFRADO_AES");
        when(pagoServicioRepository.save(any(PagoServicio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenActivo));
        when(gestoPagoTxClient.sendTx(anyString(), anyString(), anyString())).thenReturn(xmlError);
        when(xmlSerializationService.deserializeFromXml(xmlError, SendTxXmlResponse.class)).thenReturn(parsed);

        assertThrows(PagoRechazadoProveedorException.class, () -> pagoServicioService.procesarPagoServicio(request));

        // El saldo del cliente debe permanecer intacto en $1000.00
        assertEquals(new BigDecimal("1000.00"), cuentaActiva.getSaldo());
        verify(cuentaRepository, never()).save(cuentaActiva);
    }

    @Test
    @DisplayName("Debe rechazar monto manipulado cuando no coincide con el precio fijo del producto en catálogo")
    void debeRechazarMontoManipulado() {
        ProductoDocument recargaFija = ProductoDocument.builder()
                .idProducto(582)
                .idServicio(133)
                .servicio("Telcel")
                .producto("Recarga 100")
                .tipoFront(1)
                .precio(new BigDecimal("100.00"))
                .activo(true)
                .build();

        PagoServicioRequest request = PagoServicioRequest.builder()
                .numeroCuenta("4028193847")
                .idServicio(133)
                .idProducto(582)
                .precio(new BigDecimal("10.00")) // Monto manipulado distinto a $100.00
                .telefonoReferencia("5555555551")
                .bandera("SIN_REFERENCIA")
                .pin("Password123!")
                .build();

        when(cuentaRepository.findByNumeroCuentaForUpdate("4028193847")).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("Password123!", "$2a$12$hashedPassword")).thenReturn(true);
        when(productoMongoRepository.findByIdProducto(582)).thenReturn(Optional.of(recargaFija));

        assertThrows(MontoInvalidoException.class, () -> pagoServicioService.procesarPagoServicio(request));
        verify(gestoPagoTxClient, never()).sendTx(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe rechazar la operación si el saldo de la cuenta es insuficiente")
    void debeRechazarPorSaldoInsuficiente() {
        cuentaActiva.setSaldo(new BigDecimal("100.00"));

        PagoServicioRequest request = PagoServicioRequest.builder()
                .numeroCuenta("4028193847")
                .idServicio(76)
                .idProducto(205)
                .precio(new BigDecimal("350.00"))
                .telefonoReferencia("9731204028806")
                .bandera("CON_REFERENCIA")
                .pin("Password123!")
                .build();

        when(cuentaRepository.findByNumeroCuentaForUpdate("4028193847")).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("Password123!", "$2a$12$hashedPassword")).thenReturn(true);
        when(productoMongoRepository.findByIdProducto(205)).thenReturn(Optional.of(productoServicio));

        assertThrows(SaldoInsuficienteException.class, () -> pagoServicioService.procesarPagoServicio(request));
        verify(gestoPagoTxClient, never()).sendTx(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe rechazar la operación si el PIN o contraseña del cliente es incorrecto")
    void debeRechazarPorPinIncorrecto() {
        PagoServicioRequest request = PagoServicioRequest.builder()
                .numeroCuenta("4028193847")
                .idServicio(76)
                .idProducto(205)
                .precio(new BigDecimal("350.00"))
                .telefonoReferencia("9731204028806")
                .bandera("CON_REFERENCIA")
                .pin("ClaveIncorrecta!")
                .build();

        when(cuentaRepository.findByNumeroCuentaForUpdate("4028193847")).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("ClaveIncorrecta!", "$2a$12$hashedPassword")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> pagoServicioService.procesarPagoServicio(request));
        verify(gestoPagoTxClient, never()).sendTx(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Conciliación (confirmTx.do): si el proveedor confirma código 06 con autorización válida, debe descontar el saldo")
    void debeConciliarYDescontarSaldoCuandoConfirmTxIndicaAplicada() {
        PagoServicio pagoPendiente = PagoServicio.builder()
                .id(50L)
                .cliente(clienteActivo)
                .cuenta(cuentaActiva)
                .numeroCuenta("4028193847")
                .upc("GP83_TIMEOUT1")
                .idServicio(76)
                .idProducto(205)
                .montoPago(new BigDecimal("200.00"))
                .saldoAnterior(new BigDecimal("1000.00"))
                .saldoPosterior(new BigDecimal("1000.00"))
                .payloadSigned("PAYLOAD_FIRMADO_ORIGINAL")
                .estatus("PENDIENTE_CONCILIACION")
                .build();

        String xmlConfirm = "<RESPONSE><NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>" +
                "<MENSAJE><CODIGO>06</CODIGO><TEXTO>La transaccion fue aplicada exitosamente</TEXTO><ID_TX>291437188</ID_TX></MENSAJE></RESPONSE>";

        SendTxXmlResponse parsedConfirm = SendTxXmlResponse.builder()
                .numAutorizacion("100024988")
                .mensaje(MensajeXml.builder().codigo("06").texto("La transaccion fue aplicada exitosamente").idTx("291437188").build())
                .build();

        when(pagoServicioRepository.findByUpc("GP83_TIMEOUT1")).thenReturn(Optional.of(pagoPendiente));
        when(cuentaRepository.findByNumeroCuentaForUpdate("4028193847")).thenReturn(Optional.of(cuentaActiva));
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenActivo));
        when(gestoPagoTxClient.confirmTx(anyString(), anyString(), anyString())).thenReturn(xmlConfirm);
        when(xmlSerializationService.deserializeFromXml(xmlConfirm, SendTxXmlResponse.class)).thenReturn(parsedConfirm);

        ConciliarPagoRequest request = new ConciliarPagoRequest("GP83_TIMEOUT1", "4028193847");
        PagoServicioResponse response = pagoServicioService.conciliarPagoServicio(request);

        assertEquals("EXITOSA", response.getEstatus());
        assertEquals(new BigDecimal("800.00"), response.getSaldoPosterior());
        assertEquals(new BigDecimal("800.00"), cuentaActiva.getSaldo());
        assertEquals("291437188", response.getIdTxGestopago());
    }
}

