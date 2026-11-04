package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.*;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private GeneradorNumeroCuentaService generadorNumeroCuentaService;

    @InjectMocks
    private CuentaService cuentaService;

    private Cliente clienteActivo;
    private Cuenta cuentaActiva;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cuentaService, "saldoInicialDefault", new BigDecimal("1000.00"));

        clienteActivo = new Cliente();
        clienteActivo.setId(1L);
        clienteActivo.setNombre("Mario");
        clienteActivo.setActivo(true);

        cuentaActiva = new Cuenta();
        cuentaActiva.setId(10L);
        cuentaActiva.setCliente(clienteActivo);
        cuentaActiva.setNumeroCuenta("4028193847");
        cuentaActiva.setSaldo(new BigDecimal("1500.00"));
        cuentaActiva.setEstatus("ACTIVA");
    }

    @Test
    @DisplayName("Debe crear una cuenta bancaria para un cliente activo")
    void testCrearCuentaBancaria() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));
        when(generadorNumeroCuentaService.generarNumeroCuentaUnico()).thenReturn("4028193847");
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> {
            Cuenta c = i.getArgument(0);
            c.setId(10L);
            return c;
        });

        CuentaCreateRequest request = new CuentaCreateRequest();
        request.setClienteId(1L);
        request.setSaldoInicial(new BigDecimal("2000.00"));

        CuentaResponse response = cuentaService.crearCuentaBancaria(request);

        assertNotNull(response);
        assertEquals("4028193847", response.getNumeroCuenta());
        assertEquals("ACTIVA", response.getEstatus());
        assertEquals(new BigDecimal("2000.00"), response.getSaldo());
    }

    @Test
    @DisplayName("Debe rechazar la creación de cuenta para un cliente inactivo")
    void testCrearCuentaClienteInactivo() {
        clienteActivo.setActivo(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));

        CuentaCreateRequest request = new CuentaCreateRequest();
        request.setClienteId(1L);

        assertThrows(ValidacionNegocioException.class, () -> cuentaService.crearCuentaBancaria(request));
        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    @DisplayName("Debe obtener saldo de la cuenta correctamente")
    void testObtenerSaldoCuenta() {
        when(cuentaRepository.findByNumeroCuenta("4028193847")).thenReturn(Optional.of(cuentaActiva));

        SaldoResponse saldo = cuentaService.obtenerSaldoCuenta("4028193847");

        assertNotNull(saldo);
        assertEquals("4028193847", saldo.getNumeroCuenta());
        assertEquals(new BigDecimal("1500.00"), saldo.getSaldo());
        assertEquals("ACTIVA", saldo.getEstatus());
    }

    @Test
    @DisplayName("Debe listar cuentas con paginación")
    void testListarCuentasPaginadas() {
        Page<Cuenta> page = new PageImpl<>(List.of(cuentaActiva), PageRequest.of(0, 10), 1);
        when(cuentaRepository.findAll(any(Pageable.class))).thenReturn(page);

        ConsultaCuentaRequest request = ConsultaCuentaRequest.builder().page(0).size(10).build();
        Page<CuentaResponse> response = cuentaService.listarCuentasPaginadas(request);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals("4028193847", response.getContent().get(0).getNumeroCuenta());
    }

    @Test
    @DisplayName("Debe rechazar actualización con saldo negativo")
    void testActualizarCuentaSaldoNegativo() {
        when(cuentaRepository.findByNumeroCuenta("4028193847")).thenReturn(Optional.of(cuentaActiva));

        CuentaUpdateRequest update = new CuentaUpdateRequest();
        update.setNumeroCuenta("4028193847");
        update.setSaldo(new BigDecimal("-50.00"));

        assertThrows(ValidacionNegocioException.class, () -> cuentaService.actualizarCuentaParcial(update));
        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }
}
