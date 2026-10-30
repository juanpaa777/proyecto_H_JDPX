package com.proyecto.servicios.controller.pagos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.dto.pagos.ConciliarPagoRequest;
import com.proyecto.servicios.dto.pagos.ConsultaPagoServicioRequest;
import com.proyecto.servicios.dto.pagos.PagoServicioRequest;
import com.proyecto.servicios.dto.pagos.PagoServicioResponse;
import com.proyecto.servicios.exception.pagos.ProveedorExternoTimeoutException;
import com.proyecto.servicios.exception.pagos.SaldoInsuficienteException;
import com.proyecto.servicios.service.pagos.PagoServicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PagoServicioControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PagoServicioService pagoServicioService;

    @InjectMocks
    private PagoServicioController pagoServicioController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pagoServicioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/pagos-servicios/pagar debe retornar 200 OK cuando el pago es exitoso (código 01)")
    void debeRetornar200EnPagoExitoso() throws Exception {
        PagoServicioRequest request = PagoServicioRequest.builder()
                .numeroCuenta("4028193847")
                .idServicio(76)
                .idProducto(205)
                .precio(new BigDecimal("366.99"))
                .telefonoReferencia("25720381")
                .bandera("CON_REFERENCIA")
                .pin("Password123!")
                .build();

        PagoServicioResponse response = PagoServicioResponse.builder()
                .id(1L)
                .numeroCuenta("4028193847")
                .upc("GP83_ABC123")
                .estatus("EXITOSA")
                .codigoRespuesta("01")
                .montoPago(new BigDecimal("366.99"))
                .saldoAnterior(new BigDecimal("1000.00"))
                .saldoPosterior(new BigDecimal("633.01"))
                .build();

        when(pagoServicioService.procesarPagoServicio(any(PagoServicioRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/pagos-servicios/pagar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("EXITOSA"))
                .andExpect(jsonPath("$.codigoRespuesta").value("01"))
                .andExpect(jsonPath("$.saldoPosterior").value(633.01));
    }

    @Test
    @DisplayName("POST /api/v1/pagos-servicios/pagar debe retornar 400 con codigo=1 si faltan campos obligatorios")
    void debeRetornar400CuandoFallaValidacionDto() throws Exception {
        PagoServicioRequest requestInvalido = new PagoServicioRequest();

        mockMvc.perform(post("/api/v1/pagos-servicios/pagar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/pagos-servicios/pagar debe retornar 422 cuando el saldo es insuficiente")
    void debeRetornar422CuandoSaldoInsuficiente() throws Exception {
        PagoServicioRequest request = PagoServicioRequest.builder()
                .numeroCuenta("4028193847")
                .idServicio(76)
                .idProducto(205)
                .precio(new BigDecimal("500.00"))
                .telefonoReferencia("25720381")
                .bandera("CON_REFERENCIA")
                .pin("Password123!")
                .build();

        when(pagoServicioService.procesarPagoServicio(any(PagoServicioRequest.class)))
                .thenThrow(new SaldoInsuficienteException("Saldo insuficiente en la cuenta"));

        mockMvc.perform(post("/api/v1/pagos-servicios/pagar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value(422));
    }

    @Test
    @DisplayName("POST /api/v1/pagos-servicios/pagar debe retornar 504 con UPC cuando ocurre timeout en el proveedor")
    void debeRetornar504CuandoTimeoutEnProveedor() throws Exception {
        PagoServicioRequest request = PagoServicioRequest.builder()
                .numeroCuenta("4028193847")
                .idServicio(76)
                .idProducto(205)
                .precio(new BigDecimal("500.00"))
                .telefonoReferencia("25720381")
                .bandera("CON_REFERENCIA")
                .pin("Password123!")
                .build();

        when(pagoServicioService.procesarPagoServicio(any(PagoServicioRequest.class)))
                .thenThrow(new ProveedorExternoTimeoutException("GP83_TIME01", "Tiempo de espera agotado"));

        mockMvc.perform(post("/api/v1/pagos-servicios/pagar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.detalles.upc").value("GP83_TIME01"))
                .andExpect(jsonPath("$.detalles.estatus").value("PENDIENTE_CONCILIACION"));
    }

    @Test
    @DisplayName("POST /api/v1/pagos-servicios/conciliar debe retornar 200 OK al conciliar pago pendiente")
    void debeConciliarCorrectamente() throws Exception {
        ConciliarPagoRequest request = new ConciliarPagoRequest("GP83_TIME01", "4028193847");
        PagoServicioResponse response = PagoServicioResponse.builder()
                .upc("GP83_TIME01")
                .estatus("EXITOSA")
                .codigoRespuesta("01")
                .build();

        when(pagoServicioService.conciliarPagoServicio(any(ConciliarPagoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/pagos-servicios/conciliar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("EXITOSA"));
    }

    @Test
    @DisplayName("POST /api/v1/pagos-servicios/consultar debe retornar página de pagos")
    void debeConsultarPagosPaginados() throws Exception {
        ConsultaPagoServicioRequest request = ConsultaPagoServicioRequest.builder()
                .numeroCuenta("4028193847")
                .page(0)
                .size(10)
                .build();

        PagoServicioResponse item = PagoServicioResponse.builder()
                .id(1L)
                .numeroCuenta("4028193847")
                .estatus("EXITOSA")
                .build();

        when(pagoServicioService.consultarPagosPaginados(any(ConsultaPagoServicioRequest.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        mockMvc.perform(post("/api/v1/pagos-servicios/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].numeroCuenta").value("4028193847"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }
}

