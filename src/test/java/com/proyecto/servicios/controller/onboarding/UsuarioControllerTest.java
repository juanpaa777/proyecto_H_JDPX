package com.proyecto.servicios.controller.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.dto.onboarding.*;
import com.proyecto.servicios.exception.onboarding.CorreoDuplicadoException;
import com.proyecto.servicios.exception.onboarding.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.onboarding.UsuarioService;
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
class UsuarioControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    private UsuarioResponse usuarioResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        usuarioResponse = new UsuarioResponse(
                1L,
                10L,
                "usuario.test@banco.com",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /usuarios/crear - Debe crear un usuario exitosamente con status 201")
    void testCrearUsuarioExitoso() throws Exception {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setClienteId(10L);
        request.setCorreo("usuario.test@banco.com");
        request.setPassword("PasswordSeguro123!");

        when(usuarioService.crearUsuarioAcceso(any(UsuarioCreateRequest.class))).thenReturn(usuarioResponse);

        mockMvc.perform(post("/usuarios/crear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.correo", is("usuario.test@banco.com")))
                .andExpect(jsonPath("$.activo", is(true)));
    }

    @Test
    @DisplayName("POST /usuarios/crear - Debe responder 400 Bad Request y GenericResponse con formato Hasaní si fallan validaciones Bean Validation")
    void testCrearUsuarioValidacionFallida() throws Exception {
        UsuarioCreateRequest requestInvalido = new UsuarioCreateRequest();
        // correo y contraseña vacíos, clienteId null

        mockMvc.perform(post("/usuarios/crear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(1)))
                .andExpect(jsonPath("$.mensaje", startsWith("Datos no validos:")))
                .andExpect(jsonPath("$.detalles.correo", notNullValue()))
                .andExpect(jsonPath("$.detalles.password", notNullValue()))
                .andExpect(jsonPath("$.detalles.clienteId", notNullValue()));
    }

    @Test
    @DisplayName("POST /usuarios/crear - Debe responder 409 Conflict si el correo ya existe")
    void testCrearUsuarioCorreoDuplicado() throws Exception {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setClienteId(10L);
        request.setCorreo("usuario.duplicado@banco.com");
        request.setPassword("PasswordSeguro123!");

        when(usuarioService.crearUsuarioAcceso(any(UsuarioCreateRequest.class)))
                .thenThrow(new CorreoDuplicadoException("usuario.duplicado@banco.com"));

        mockMvc.perform(post("/usuarios/crear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is(409)))
                .andExpect(jsonPath("$.mensaje", containsString("usuario.duplicado@banco.com")));
    }

    @Test
    @DisplayName("POST /usuarios/consultar - Debe retornar resultados dinámicos y paginados con status 200")
    void testConsultarUsuariosPaginados() throws Exception {
        ConsultaUsuarioRequest request = ConsultaUsuarioRequest.builder()
                .correo("banco.com")
                .activo(true)
                .page(0)
                .size(10)
                .build();

        PageImpl<UsuarioResponse> pagina = new PageImpl<>(List.of(usuarioResponse), PageRequest.of(0, 10), 1);
        when(usuarioService.buscarUsuariosPaginados(any(ConsultaUsuarioRequest.class))).thenReturn(pagina);

        mockMvc.perform(post("/usuarios/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.paginaActual", is(0)))
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.esPrimera", is(true)))
                .andExpect(jsonPath("$.esUltima", is(true)));
    }

    @Test
    @DisplayName("POST /usuarios/detalle - Debe obtener detalle de usuario por ID en RequestBody")
    void testObtenerDetalleUsuario() throws Exception {
        DetalleUsuarioRequest request = new DetalleUsuarioRequest(1L);
        when(usuarioService.obtenerUsuarioDetalle(any(DetalleUsuarioRequest.class))).thenReturn(usuarioResponse);

        mockMvc.perform(post("/usuarios/detalle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.correo", is("usuario.test@banco.com")));
    }

    @Test
    @DisplayName("POST /usuarios/detalle - Debe responder 404 Not Found si el usuario no existe")
    void testObtenerDetalleUsuarioNoExiste() throws Exception {
        DetalleUsuarioRequest request = new DetalleUsuarioRequest(999L);
        when(usuarioService.obtenerUsuarioDetalle(any(DetalleUsuarioRequest.class)))
                .thenThrow(new UsuarioNoEncontradoException(999L));

        mockMvc.perform(post("/usuarios/detalle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.mensaje", containsString("999")));
    }

    @Test
    @DisplayName("PUT /usuarios/actualizar - Debe actualizar usuario con status 200")
    void testActualizarUsuario() throws Exception {
        UsuarioUpdateRequest request = UsuarioUpdateRequest.builder()
                .id(1L)
                .correo("nuevo.correo@banco.com")
                .activo(true)
                .build();

        when(usuarioService.actualizarUsuario(any(UsuarioUpdateRequest.class))).thenReturn(usuarioResponse);

        mockMvc.perform(put("/usuarios/actualizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @DisplayName("PUT /usuarios/eliminar - Debe ejecutar baja lógica y retornar GenericResponse con código 0")
    void testEliminarUsuario() throws Exception {
        EliminaUsuarioRequest request = new EliminaUsuarioRequest(1L);
        when(usuarioService.darDeBajaUsuario(any(EliminaUsuarioRequest.class)))
                .thenReturn(new GenericResponse(0, "Usuario desactivado correctamente"));

        mockMvc.perform(put("/usuarios/eliminar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(0)))
                .andExpect(jsonPath("$.mensaje", is("Usuario desactivado correctamente")));
    }
}
