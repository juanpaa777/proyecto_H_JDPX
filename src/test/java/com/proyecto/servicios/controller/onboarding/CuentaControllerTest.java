package com.proyecto.servicios.controller.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.dto.onboarding.*;
import com.proyecto.servicios.service.onboarding.CuentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CuentaControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private CuentaController cuentaController;

    private CuentaResponse cuentaResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cuentaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        cuentaResponse = new CuentaResponse(
                10L,
                1L,
                "4028193847",
                new BigDecimal("5000.00"),
                "ACTIVA",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /cuentas/crear - Debe crear una cuenta asociada con status 201")
    void testCrearCuentaExitoso() throws Exception {
        CuentaCreateRequest request = new CuentaCreateRequest();
        request.setClienteId(1L);
        request.setSaldoInicial(new BigDecimal("5000.00"));

        when(cuentaService.crearCuentaBancaria(any(CuentaCreateRequest.class))).thenReturn(cuentaResponse);

        mockMvc.perform(post("/cuentas/crear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCuenta", is("4028193847")))
                .andExpect(jsonPath("$.estatus", is("ACTIVA")));
    }

    @Test
    @DisplayName("POST /cuentas/consultar - Debe listar cuentas paginadas con status 200")
    void testConsultarCuentasPaginadas() throws Exception {
        ConsultaCuentaRequest request = ConsultaCuentaRequest.builder()
                .clienteId(1L)
                .page(0)
                .size(10)
                .build();

        PageImpl<CuentaResponse> pagina = new PageImpl<>(List.of(cuentaResponse), PageRequest.of(0, 10), 1);
        when(cuentaService.listarCuentasPaginadas(any(ConsultaCuentaRequest.class))).thenReturn(pagina);

        mockMvc.perform(post("/cuentas/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.esPrimera", is(true)));
    }

    @Test
    @DisplayName("POST /cuentas/detalle - Debe retornar detalle de cuenta por RequestBody")
    void testConsultarDetalleCuenta() throws Exception {
        DetalleCuentaRequest request = new DetalleCuentaRequest("4028193847");
        when(cuentaService.obtenerCuentaDetalle(any(DetalleCuentaRequest.class))).thenReturn(cuentaResponse);

        mockMvc.perform(post("/cuentas/detalle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta", is("4028193847")));
    }

    @Test
    @DisplayName("POST /cuentas/saldo - Debe retornar saldo en vivo de la cuenta")
    void testConsultarSaldoCuenta() throws Exception {
        DetalleCuentaRequest request = new DetalleCuentaRequest("4028193847");
        SaldoResponse saldoResponse = new SaldoResponse("4028193847", new BigDecimal("5000.00"), "ACTIVA");
        when(cuentaService.obtenerSaldoDetalle(any(DetalleCuentaRequest.class))).thenReturn(saldoResponse);

        mockMvc.perform(post("/cuentas/saldo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta", is("4028193847")))
                .andExpect(jsonPath("$.saldo", is(5000.00)));
    }

    @Test
    @DisplayName("PUT /cuentas/actualizar - Debe actualizar cuenta por RequestBody")
    void testActualizarCuenta() throws Exception {
        CuentaUpdateRequest request = new CuentaUpdateRequest();
        request.setNumeroCuenta("4028193847");
        request.setEstatus("BLOQUEADA");

        cuentaResponse.setEstatus("BLOQUEADA");
        when(cuentaService.actualizarCuentaParcial(any(CuentaUpdateRequest.class))).thenReturn(cuentaResponse);

        mockMvc.perform(put("/cuentas/actualizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus", is("BLOQUEADA")));
    }
}
