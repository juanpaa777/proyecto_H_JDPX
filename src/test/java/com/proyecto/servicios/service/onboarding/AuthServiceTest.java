package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.LoginRequest;
import com.proyecto.servicios.dto.onboarding.LoginResponse;
import com.proyecto.servicios.dto.onboarding.SessionStatusResponse;
import com.proyecto.servicios.dto.onboarding.Verify2faRequest;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Codigo2fa;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.SesionActiva;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.exception.onboarding.SesionConflictoException;
import com.proyecto.servicios.exception.onboarding.SesionInvalidaException;
import com.proyecto.servicios.exception.onboarding.UsuarioInactivoException;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.repositorys.onboarding.Codigo2faRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.SesionActivaRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.security.onboarding.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SesionActivaRepository sesionActivaRepository;

    @Mock
    private Codigo2faRepository codigo2faRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    private Usuario usuario;
    private Cliente cliente;
    private Cuenta cuenta;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                usuarioRepository,
                passwordEncoder,
                sesionActivaRepository,
                codigo2faRepository,
                cuentaRepository,
                jwtService
        );

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

        cuenta = new Cuenta();
        cuenta.setId(100L);
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta("4028193847");
        cuenta.setSaldo(new BigDecimal("2500.00"));
        cuenta.setEstatus("ACTIVA");
    }

    @Test
    @DisplayName("Debe iniciar sesión exitosamente y generar token JWT y sesión activa")
    void testLoginExitoso() {
        LoginRequest request = new LoginRequest();
        request.setCorreo("ana.martinez@banco.com");
        request.setPassword("PasswordSegura123!");

        when(usuarioRepository.findByCorreo("ana.martinez@banco.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("PasswordSegura123!", "$2a$12$hashedPassword")).thenReturn(true);
        when(sesionActivaRepository.findByUsuarioIdAndActivaTrue(1L)).thenReturn(Collections.emptyList());
        when(jwtService.generateToken(eq(10L), eq("ana.martinez@banco.com"), anyString(), anyString())).thenReturn("jwt-token-valido");
        when(cuentaRepository.findByClienteId(eq(10L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(cuenta)));

        LoginResponse response = authService.autenticarUsuario(request);

        assertNotNull(response);
        assertEquals("jwt-token-valido", response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals("ana.martinez@banco.com", response.getCorreo());
        assertEquals("4028193847", response.getNumeroCuenta());
        verify(sesionActivaRepository).save(any(SesionActiva.class));
    }

    @Test
    @DisplayName("Debe lanzar CredencialesInvalidasException si la contraseña no coincide")
    void testLoginPasswordIncorrecto() {
        LoginRequest request = new LoginRequest();
        request.setCorreo("ana.martinez@banco.com");
        request.setPassword("PasswordErronea");

        when(usuarioRepository.findByCorreo("ana.martinez@banco.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("PasswordErronea", "$2a$12$hashedPassword")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.autenticarUsuario(request));
    }

    @Test
    @DisplayName("Debe lanzar UsuarioInactivoException si el usuario o cliente se encuentra inactivo")
    void testLoginUsuarioInactivo() {
        usuario.setActivo(false);

        LoginRequest request = new LoginRequest();
        request.setCorreo("ana.martinez@banco.com");
        request.setPassword("PasswordSegura123!");

        when(usuarioRepository.findByCorreo("ana.martinez@banco.com")).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioInactivoException.class, () -> authService.autenticarUsuario(request));

        usuario.setActivo(true);
        cliente.setActivo(false);
        assertThrows(UsuarioInactivoException.class, () -> authService.autenticarUsuario(request));
    }

    @Test
    @DisplayName("Debe detectar conflicto de sesión única (409) cuando ya existe sesión activa en otro dispositivo")
    void testDetectarConflictoSesionActiva() {
        SesionActiva sesionPrevia = SesionActiva.builder()
                .id(50L)
                .usuarioId(1L)
                .clienteId(10L)
                .tokenJti("jti-mobile-123")
                .plataforma("MOBILE")
                .dispositivoInfo("Flutter Android")
                .activa(true)
                .fechaInicio(LocalDateTime.now().minusMinutes(10))
                .fechaExpiracion(LocalDateTime.now().plusHours(23))
                .build();

        LoginRequest request = LoginRequest.builder()
                .correo("ana.martinez@banco.com")
                .password("PasswordSegura123!")
                .plataforma("WEB")
                .forzarCierreSesionPrevia(false)
                .build();

        when(usuarioRepository.findByCorreo("ana.martinez@banco.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("PasswordSegura123!", "$2a$12$hashedPassword")).thenReturn(true);
        when(sesionActivaRepository.findByUsuarioIdAndActivaTrue(1L)).thenReturn(List.of(sesionPrevia));

        SesionConflictoException ex = assertThrows(SesionConflictoException.class, () -> authService.autenticarUsuario(request));
        assertEquals("MOBILE", ex.getPlataformaActiva());
        assertEquals("Flutter Android", ex.getDispositivoActivo());
    }

    @Test
    @DisplayName("Debe generar desafío 2FA cuando requerir2fa es true y controlar límite de solicitudes")
    void testGenerarDesafio2faYRateLimit() {
        LoginRequest request = LoginRequest.builder()
                .correo("ana.martinez@banco.com")
                .password("PasswordSegura123!")
                .plataforma("MOBILE")
                .dispositivo("Flutter App")
                .requerir2fa(true)
                .build();

        when(usuarioRepository.findByCorreo("ana.martinez@banco.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("PasswordSegura123!", "$2a$12$hashedPassword")).thenReturn(true);
        when(sesionActivaRepository.findByUsuarioIdAndActivaTrue(1L)).thenReturn(Collections.emptyList());
        when(codigo2faRepository.countByUsuarioIdAndFechaCreacionAfter(eq(1L), any(LocalDateTime.class))).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$otpHash");

        LoginResponse response = authService.autenticarUsuario(request);

        assertTrue(response.getRequiere2fa());
        assertNotNull(response.getDesafioId());
        assertNull(response.getCodigoOtpDemo());
        verify(codigo2faRepository).save(any(Codigo2fa.class));

        // Simular exceso de solicitudes (Rate limit)
        when(codigo2faRepository.countByUsuarioIdAndFechaCreacionAfter(eq(1L), any(LocalDateTime.class))).thenReturn(5L);
        assertThrows(ValidacionNegocioException.class, () -> authService.autenticarUsuario(request));
    }

    @Test
    @DisplayName("Debe verificar código 2FA correctamente, revocar sesión anterior y emitir JWT")
    void testVerificarCodigo2faExitosoYRevocarSesionPrevia() {
        Codigo2fa desafio = Codigo2fa.builder()
                .id(1L)
                .usuarioId(1L)
                .desafioId("desafio-uuid-1")
                .codigoHash("$2a$12$otpHash")
                .plataforma("WEB")
                .dispositivoInfo("Chrome Web")
                .intentosFallidos(0)
                .usado(false)
                .forzarCierrePrevia(true)
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .build();

        SesionActiva sesionAnterior = SesionActiva.builder()
                .id(50L)
                .usuarioId(1L)
                .clienteId(10L)
                .tokenJti("jti-anterior")
                .plataforma("MOBILE")
                .dispositivoInfo("Flutter")
                .activa(true)
                .fechaInicio(LocalDateTime.now().minusMinutes(30))
                .fechaExpiracion(LocalDateTime.now().plusHours(20))
                .build();

        when(codigo2faRepository.findByDesafioId("desafio-uuid-1")).thenReturn(Optional.of(desafio));
        when(passwordEncoder.matches("123456", "$2a$12$otpHash")).thenReturn(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(sesionActivaRepository.findByUsuarioIdAndActivaTrue(1L)).thenReturn(List.of(sesionAnterior));
        when(jwtService.generateToken(eq(10L), eq("ana.martinez@banco.com"), anyString(), anyString())).thenReturn("jwt-2fa-ok");
        when(cuentaRepository.findByClienteId(eq(10L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(cuenta)));

        Verify2faRequest verifyRequest = Verify2faRequest.builder()
                .desafioId("desafio-uuid-1")
                .codigoOtp("123456")
                .forzarCierreSesionPrevia(true)
                .build();

        LoginResponse response = authService.verificarCodigo2fa(verifyRequest);

        assertNotNull(response);
        assertFalse(response.getRequiere2fa());
        assertEquals("jwt-2fa-ok", response.getToken());
        assertTrue(desafio.getUsado());
        assertFalse(sesionAnterior.getActiva());
    }

    @Test
    @DisplayName("Debe controlar intentos fallidos, expiración y reutilización en 2FA")
    void testValidacionesSeguridad2fa() {
        Codigo2fa desafio = Codigo2fa.builder()
                .id(2L)
                .usuarioId(1L)
                .desafioId("desafio-uuid-2")
                .codigoHash("$2a$12$otpHash")
                .plataforma("WEB")
                .dispositivoInfo("Chrome")
                .intentosFallidos(0)
                .usado(false)
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .build();

        when(codigo2faRepository.findByDesafioId("desafio-uuid-2")).thenReturn(Optional.of(desafio));
        when(passwordEncoder.matches("000000", "$2a$12$otpHash")).thenReturn(false);

        Verify2faRequest badReq = new Verify2faRequest("desafio-uuid-2", "000000", false);
        assertThrows(CredencialesInvalidasException.class, () -> authService.verificarCodigo2fa(badReq));
        assertEquals(1, desafio.getIntentosFallidos());

        // Simular bloqueo por 3 intentos fallidos
        desafio.setIntentosFallidos(3);
        assertThrows(CredencialesInvalidasException.class, () -> authService.verificarCodigo2fa(badReq));

        // Simular código expirado
        desafio.setIntentosFallidos(0);
        desafio.setFechaExpiracion(LocalDateTime.now().minusMinutes(1));
        assertThrows(CredencialesInvalidasException.class, () -> authService.verificarCodigo2fa(badReq));

        // Simular código ya usado
        desafio.setUsado(true);
        assertThrows(CredencialesInvalidasException.class, () -> authService.verificarCodigo2fa(badReq));
    }

    @Test
    @DisplayName("Debe consultar estado de sesión activa y cerrar sesión correctamente")
    void testConsultarEstadoYCerrarSesion() {
        SesionActiva sesion = SesionActiva.builder()
                .id(80L)
                .usuarioId(1L)
                .clienteId(10L)
                .tokenJti("jti-activo-999")
                .plataforma("WEB")
                .dispositivoInfo("Edge Browser")
                .activa(true)
                .fechaInicio(LocalDateTime.now().minusMinutes(5))
                .fechaExpiracion(LocalDateTime.now().plusHours(23))
                .build();

        when(jwtService.extractJti("token-jwt-999")).thenReturn("jti-activo-999");
        when(sesionActivaRepository.findByTokenJti("jti-activo-999")).thenReturn(Optional.of(sesion));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByClienteId(eq(10L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(cuenta)));

        SessionStatusResponse status = authService.consultarEstadoSesion("Bearer token-jwt-999");
        assertTrue(status.isActiva());
        assertEquals("4028193847", status.getNumeroCuenta());

        // Cerrar sesión
        authService.cerrarSesion("Bearer token-jwt-999");
        assertFalse(sesion.getActiva());

        // Consultar sesión ya cerrada debe lanzar SesionInvalidaException
        assertThrows(SesionInvalidaException.class, () -> authService.consultarEstadoSesion("Bearer token-jwt-999"));
    }
}
