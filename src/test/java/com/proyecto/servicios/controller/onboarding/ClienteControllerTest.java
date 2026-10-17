package com.proyecto.servicios.controller.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.dto.onboarding.*;
import com.proyecto.servicios.service.onboarding.ClienteService;
import com.proyecto.servicios.service.onboarding.OnboardingService;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private OnboardingService onboardingService;

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private ClienteResponse clienteResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        clienteResponse = new ClienteResponse();
        clienteResponse.setId(1L);
        clienteResponse.setNombre("Carlos");
        clienteResponse.setApellidoPaterno("Hernández");
        clienteResponse.setApellidoMaterno("Gómez");
        clienteResponse.setNombreCompleto("Carlos Hernández Gómez");
        clienteResponse.setCurp("HEGC900101HDFRRN09");
        clienteResponse.setRfc("HEGC900101XYZ");
        clienteResponse.setCorreo("carlos.h@banco.com");
        clienteResponse.setActivo(true);
        clienteResponse.setCuentas(new ArrayList<>());
    }

    @Test
    @DisplayName("POST /clientes/registrar - Debe crear cliente con status 201")
    void testRegistrarClienteExitoso() throws Exception {
        RegistroClienteRequest request = new RegistroClienteRequest();
        request.setNombre("Carlos");
        request.setApellidoPaterno("Hernández");
        request.setApellidoMaterno("Gómez");
        request.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        request.setCurp("HEGC900101HDFRRN09");
        request.setRfc("HEGC900101XYZ");
        request.setSexo("MASCULINO");
        request.setNacionalidad("Mexicana");
        request.setEstadoCivil("SOLTERO");
        request.setCorreo("carlos.h@banco.com");
        request.setTelefonoMovil("5512345678");
        request.setOcupacion("Ingeniero");
        request.setEmpresa("Tech");
        request.setIngresoMensual(new java.math.BigDecimal("30000.00"));
        request.setPassword("PasswordSeguro123!");

        DomicilioDto dom = new DomicilioDto();
        dom.setCalle("Insurgentes Sur");
        dom.setNumeroExterior("100");
        dom.setColonia("Del Valle");
        dom.setMunicipio("Benito Juárez");
        dom.setEstado("CDMX");
        dom.setCodigoPostal("03100");
        request.setDomicilio(dom);

        when(onboardingService.registrarNuevoCliente(any(RegistroClienteRequest.class))).thenReturn(clienteResponse);

        mockMvc.perform(post("/clientes/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.curp", is("HEGC900101HDFRRN09")));
    }

    @Test
    @DisplayName("POST /clientes/registrar - Falla si faltan campos obligatorios y responde 400 con GenericResponse")
    void testRegistrarClienteInvalido() throws Exception {
        RegistroClienteRequest requestInvalido = new RegistroClienteRequest();

        mockMvc.perform(post("/clientes/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(1)))
                .andExpect(jsonPath("$.mensaje", startsWith("Datos no validos:")));
    }

    @Test
    @DisplayName("POST /clientes/consultar - Debe retornar resultados dinámicos y paginados")
    void testConsultarClientesPaginados() throws Exception {
        ConsultaClienteRequest request = ConsultaClienteRequest.builder()
                .nombre("Carlos")
                .page(0)
                .size(10)
                .build();

        PageImpl<ClienteResponse> pagina = new PageImpl<>(List.of(clienteResponse), PageRequest.of(0, 10), 1);
        when(clienteService.buscarClientesPaginados(any(ConsultaClienteRequest.class))).thenReturn(pagina);

        mockMvc.perform(post("/clientes/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.esPrimera", is(true)));
    }

    @Test
    @DisplayName("POST /clientes/detalle - Debe obtener detalle de cliente en RequestBody")
    void testConsultarDetalleCliente() throws Exception {
        DetalleClienteRequest request = DetalleClienteRequest.builder().id(1L).build();
        when(clienteService.obtenerClienteDetalle(any(DetalleClienteRequest.class))).thenReturn(clienteResponse);

        mockMvc.perform(post("/clientes/detalle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nombre", is("Carlos")));
    }

    @Test
    @DisplayName("PUT /clientes/actualizar - Debe actualizar cliente con status 200")
    void testActualizarCliente() throws Exception {
        ClienteUpdateRequest request = new ClienteUpdateRequest();
        request.setId(1L);
        request.setNombre("Carlos Alberto");

        when(clienteService.actualizarClienteParcial(any(ClienteUpdateRequest.class))).thenReturn(clienteResponse);

        mockMvc.perform(put("/clientes/actualizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @DisplayName("PUT /clientes/baja - Debe dar de baja en cascada mediante DTO en RequestBody")
    void testBajaLogicaCliente() throws Exception {
        EliminaClienteRequest request = new EliminaClienteRequest(1L);
        clienteResponse.setActivo(false);
        when(clienteService.darDeBajaClienteLogica(any(EliminaClienteRequest.class))).thenReturn(clienteResponse);

        mockMvc.perform(put("/clientes/baja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo", is(false)));
    }
}
