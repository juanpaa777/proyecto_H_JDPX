package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.ClienteResponse;
import com.proyecto.servicios.dto.onboarding.DomicilioDto;
import com.proyecto.servicios.dto.onboarding.RegistroClienteRequest;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.onboarding.CorreoDuplicadoException;
import com.proyecto.servicios.exception.onboarding.CurpDuplicadaException;
import com.proyecto.servicios.exception.onboarding.RfcDuplicadoException;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.DomicilioRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
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
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private DomicilioRepository domicilioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private GeneradorNumeroCuentaService generadorNumeroCuentaService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OnboardingService onboardingService;

    private RegistroClienteRequest requestValido;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(onboardingService, "saldoInicialDefault", new BigDecimal("1000.00"));

        requestValido = new RegistroClienteRequest();
        requestValido.setNombre("Juan");
        requestValido.setApellidoPaterno("Pérez");
        requestValido.setApellidoMaterno("López");
        requestValido.setFechaNacimiento(LocalDate.of(1995, 5, 20));
        requestValido.setCurp("PELJ950520HDFRRN01");
        requestValido.setRfc("PELJ950520ABC");
        requestValido.setSexo("MASCULINO");
        requestValido.setNacionalidad("Mexicana");
        requestValido.setEstadoCivil("SOLTERO");
        requestValido.setCorreo("juan.perez@example.com");
        requestValido.setTelefonoMovil("5512345678");
        requestValido.setOcupacion("Desarrollador");
        requestValido.setEmpresa("Tech Corp");
        requestValido.setIngresoMensual(new BigDecimal("25000.00"));
        requestValido.setPassword("Password123!");

        DomicilioDto dom = new DomicilioDto();
        dom.setCalle("Av. Reforma");
        dom.setNumeroExterior("123");
        dom.setColonia("Juárez");
        dom.setMunicipio("Cuauhtémoc");
        dom.setEstado("CDMX");
        dom.setCodigoPostal("06600");
        dom.setPais("México");
        requestValido.setDomicilio(dom);
    }

    @Test
    @DisplayName("Debe registrar exitosamente un cliente con su cuenta y usuario automático")
    void testRegistrarClienteExitoso() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        when(domicilioRepository.save(any(Domicilio.class))).thenAnswer(invocation -> {
            Domicilio d = invocation.getArgument(0);
            d.setId(10L);
            return d;
        });

        when(generadorNumeroCuentaService.generarNumeroCuentaUnico()).thenReturn("4028193847");
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> {
            Cuenta c = invocation.getArgument(0);
            c.setId(100L);
            return c;
        });

        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPasswordExample");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(50L);
            return u;
        });

        ClienteResponse response = onboardingService.registrarCliente(requestValido);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Juan Pérez López", response.getNombreCompleto());
        assertEquals("PELJ950520HDFRRN01", response.getCurp());
        assertEquals(1, response.getCuentas().size());
        assertEquals("4028193847", response.getCuentas().get(0).getNumeroCuenta());
        assertEquals("ACTIVA", response.getCuentas().get(0).getEstatus());
        assertEquals(new BigDecimal("1000.00"), response.getCuentas().get(0).getSaldo());
        assertNotNull(response.getUsuario());
        assertTrue(response.getUsuario().getActivo());

        verify(clienteRepository).save(any(Cliente.class));
        verify(cuentaRepository).save(any(Cuenta.class));
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe lanzar CurpDuplicadaException si la CURP ya existe")
    void testRegistrarClienteCurpDuplicada() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(true);

        assertThrows(CurpDuplicadaException.class, () -> onboardingService.registrarCliente(requestValido));
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe lanzar RfcDuplicadoException si el RFC ya existe")
    void testRegistrarClienteRfcDuplicado() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(true);

        assertThrows(RfcDuplicadoException.class, () -> onboardingService.registrarCliente(requestValido));
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe lanzar CorreoDuplicadoException si el correo ya existe")
    void testRegistrarClienteCorreoDuplicado() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> onboardingService.registrarCliente(requestValido));
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe lanzar ValidacionNegocioException si el cliente es menor de edad")
    void testRegistrarClienteMenorDeEdad() {
        requestValido.setFechaNacimiento(LocalDate.now().minusYears(17));

        assertThrows(ValidacionNegocioException.class, () -> onboardingService.registrarCliente(requestValido));
        verify(clienteRepository, never()).save(any(Cliente.class));
    }
}
