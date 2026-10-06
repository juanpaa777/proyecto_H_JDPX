package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.LoginRequest;
import com.proyecto.servicios.dto.onboarding.LoginResponse;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.exception.onboarding.UsuarioInactivoException;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(10L);
        cliente.setNombre("Ana");
        cliente.setApellidoPaterno("Martínez");
        cliente.setActivo(true);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCliente(cliente);
        usuario.setCorreo("ana.martinez@banco.com");
        usuario.setPassword("$2a$12$hashedPassword");
        usuario.setActivo(true);
    }

    @Test
    @DisplayName("Debe iniciar sesión exitosamente y generar token con headers listos")
    void testLoginExitoso() {
        LoginRequest request = new LoginRequest();
        request.setCorreo("ana.martinez@banco.com");
        request.setPassword("PasswordSegura123!");

        when(usuarioRepository.findByCorreo("ana.martinez@banco.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("PasswordSegura123!", "$2a$12$hashedPassword")).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals("ana.martinez@banco.com", response.getCorreo());
    }

    @Test
    @DisplayName("Debe lanzar CredencialesInvalidasException si la contraseña no coincide")
    void testLoginPasswordIncorrecto() {
        LoginRequest request = new LoginRequest();
        request.setCorreo("ana.martinez@banco.com");
        request.setPassword("PasswordErronea");

        when(usuarioRepository.findByCorreo("ana.martinez@banco.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("PasswordErronea", "$2a$12$hashedPassword")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Debe lanzar UsuarioInactivoException si el usuario se encuentra inactivo")
    void testLoginUsuarioInactivo() {
        usuario.setActivo(false);

        LoginRequest request = new LoginRequest();
        request.setCorreo("ana.martinez@banco.com");
        request.setPassword("PasswordSegura123!");

        when(usuarioRepository.findByCorreo("ana.martinez@banco.com")).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioInactivoException.class, () -> authService.login(request));
    }
}
