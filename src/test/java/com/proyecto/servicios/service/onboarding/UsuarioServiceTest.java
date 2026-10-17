package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.*;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.onboarding.CorreoDuplicadoException;
import com.proyecto.servicios.exception.onboarding.UsuarioNoEncontradoException;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Cliente clienteActivo;
    private Usuario usuarioExistente;

    @BeforeEach
    void setUp() {
        clienteActivo = new Cliente();
        clienteActivo.setId(10L);
        clienteActivo.setNombre("Laura");
        clienteActivo.setActivo(true);

        usuarioExistente = new Usuario();
        usuarioExistente.setId(1L);
        usuarioExistente.setCliente(clienteActivo);
        usuarioExistente.setCorreo("laura@banco.com");
        usuarioExistente.setPassword("$2a$12$hashedPassword");
        usuarioExistente.setActivo(true);
    }

    @Test
    @DisplayName("Debe crear exitosamente un usuario de acceso")
    void testCrearUsuarioExitoso() {
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteActivo));
        when(usuarioRepository.findByClienteId(10L)).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo("laura@banco.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$crypted");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setClienteId(10L);
        request.setCorreo("laura@banco.com");
        request.setPassword("PasswordSeguro123!");

        UsuarioResponse response = usuarioService.crearUsuarioAcceso(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("laura@banco.com", response.getCorreo());
        assertTrue(response.getActivo());
    }

    @Test
    @DisplayName("Debe rechazar la creación si el cliente está inactivo")
    void testCrearUsuarioClienteInactivo() {
        clienteActivo.setActivo(false);
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteActivo));

        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setClienteId(10L);
        request.setCorreo("laura@banco.com");
        request.setPassword("PasswordSeguro123!");

        assertThrows(ValidacionNegocioException.class, () -> usuarioService.crearUsuarioAcceso(request));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe rechazar la creación si el correo ya existe")
    void testCrearUsuarioCorreoDuplicado() {
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteActivo));
        when(usuarioRepository.findByClienteId(10L)).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo("laura@banco.com")).thenReturn(true);

        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setClienteId(10L);
        request.setCorreo("laura@banco.com");
        request.setPassword("PasswordSeguro123!");

        assertThrows(CorreoDuplicadoException.class, () -> usuarioService.crearUsuarioAcceso(request));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe buscar usuarios dinámicamente y con paginación")
    @SuppressWarnings("unchecked")
    void testBuscarUsuariosPaginados() {
        Page<Usuario> pagina = new PageImpl<>(List.of(usuarioExistente), PageRequest.of(0, 10), 1);
        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);

        ConsultaUsuarioRequest request = ConsultaUsuarioRequest.builder().correo("laura").page(0).size(10).build();
        Page<UsuarioResponse> response = usuarioService.buscarUsuariosPaginados(request);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals("laura@banco.com", response.getContent().get(0).getCorreo());
    }

    @Test
    @DisplayName("Debe obtener detalle de usuario por ID")
    void testObtenerUsuarioDetalle() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));

        DetalleUsuarioRequest request = new DetalleUsuarioRequest(1L);
        UsuarioResponse response = usuarioService.obtenerUsuarioDetalle(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("laura@banco.com", response.getCorreo());
    }

    @Test
    @DisplayName("Debe lanzar UsuarioNoEncontradoException si el usuario no existe")
    void testObtenerUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        DetalleUsuarioRequest request = new DetalleUsuarioRequest(99L);
        assertThrows(UsuarioNoEncontradoException.class, () -> usuarioService.obtenerUsuarioDetalle(request));
    }

    @Test
    @DisplayName("Debe actualizar usuario correctamente")
    void testActualizarUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.existsByCorreo("nuevo@banco.com")).thenReturn(false);
        when(passwordEncoder.encode("NuevaPassword123!")).thenReturn("$2a$12$newhash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioUpdateRequest request = UsuarioUpdateRequest.builder()
                .id(1L)
                .correo("nuevo@banco.com")
                .password("NuevaPassword123!")
                .activo(true)
                .build();

        UsuarioResponse response = usuarioService.actualizarUsuario(request);

        assertNotNull(response);
        assertEquals("nuevo@banco.com", response.getCorreo());
    }

    @Test
    @DisplayName("Debe aplicar baja lógica al usuario")
    void testDarDeBajaUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        EliminaUsuarioRequest request = new EliminaUsuarioRequest(1L);
        GenericResponse response = usuarioService.darDeBajaUsuario(request);

        assertNotNull(response);
        assertEquals(0, response.getCodigo());
        assertFalse(usuarioExistente.getActivo());
    }
}
