package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.ClienteResponse;
import com.proyecto.servicios.dto.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.dto.onboarding.ConsultaClienteRequest;
import com.proyecto.servicios.dto.onboarding.DetalleClienteRequest;
import com.proyecto.servicios.dto.onboarding.EliminaClienteRequest;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.onboarding.ModificacionNoPermitidaException;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente clienteExistente;

    @BeforeEach
    void setUp() {
        clienteExistente = new Cliente();
        clienteExistente.setId(1L);
        clienteExistente.setNombre("Carlos");
        clienteExistente.setApellidoPaterno("Hernández");
        clienteExistente.setApellidoMaterno("Gómez");
        clienteExistente.setCurp("HEGC900101HDFRRN09");
        clienteExistente.setRfc("HEGC900101XYZ");
        clienteExistente.setCorreo("carlos.h@example.com");
        clienteExistente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        clienteExistente.setActivo(true);
        clienteExistente.setCuentas(new ArrayList<>());

        Cuenta cuenta = new Cuenta();
        cuenta.setId(10L);
        cuenta.setCliente(clienteExistente);
        cuenta.setNumeroCuenta("5551234567");
        cuenta.setSaldo(new BigDecimal("5000.00"));
        cuenta.setEstatus("ACTIVA");
        clienteExistente.getCuentas().add(cuenta);

        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setCliente(clienteExistente);
        usuario.setCorreo("carlos.h@example.com");
        usuario.setActivo(true);
        clienteExistente.setUsuario(usuario);
    }

    @Test
    @DisplayName("Debe listar clientes paginados utilizando Specification dinámica")
    @SuppressWarnings("unchecked")
    void testListarClientesPaginados() {
        Page<Cliente> pagina = new PageImpl<>(List.of(clienteExistente), PageRequest.of(0, 10), 1);
        when(clienteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);

        ConsultaClienteRequest request = ConsultaClienteRequest.builder().nombre("Carlos").build();
        Page<ClienteResponse> resultado = clienteService.buscarClientesPaginados(request);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals("Carlos", resultado.getContent().get(0).getNombre());
    }

    @Test
    @DisplayName("Debe obtener un cliente por ID correctamente")
    void testObtenerClientePorId() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente));

        ClienteResponse response = clienteService.obtenerClientePorId(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("HEGC900101HDFRRN09", response.getCurp());
    }

    @Test
    @DisplayName("Debe rechazar la actualización si se intenta modificar la CURP")
    void testActualizarClienteCurpInmutable() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente));

        ClienteUpdateRequest updateRequest = new ClienteUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setCurp("OTRACURP1234567890");

        assertThrows(ModificacionNoPermitidaException.class, () -> clienteService.actualizarClienteParcial(updateRequest));
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe rechazar la actualización si se intenta modificar el RFC")
    void testActualizarClienteRfcInmutable() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente));

        ClienteUpdateRequest updateRequest = new ClienteUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setRfc("OTRORFC123456");

        assertThrows(ModificacionNoPermitidaException.class, () -> clienteService.actualizarClienteParcial(updateRequest));
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe desactivar cliente, sus cuentas y su usuario al ejecutar baja lógica")
    void testBajaLogicaCascada() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        EliminaClienteRequest eliminaRequest = new EliminaClienteRequest(1L);
        ClienteResponse response = clienteService.darDeBajaClienteLogica(eliminaRequest);

        assertNotNull(response);
        assertFalse(clienteExistente.getActivo());
        assertEquals("INACTIVA", clienteExistente.getCuentas().get(0).getEstatus());
        assertFalse(clienteExistente.getUsuario().getActivo());

        verify(cuentaRepository).save(any(Cuenta.class));
        verify(usuarioRepository).save(any(Usuario.class));
        verify(clienteRepository).save(clienteExistente);
    }

    @Test
    @DisplayName("Debe consultar detalle de cliente por distintos criterios")
    void testObtenerClienteDetalle() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente));
        when(clienteRepository.findByCurp("HEGC900101HDFRRN09")).thenReturn(Optional.of(clienteExistente));
        when(clienteRepository.findByRfc("HEGC900101XYZ")).thenReturn(Optional.of(clienteExistente));
        when(clienteRepository.findByNumeroCuenta("5551234567")).thenReturn(Optional.of(clienteExistente));

        assertNotNull(clienteService.obtenerClienteDetalle(DetalleClienteRequest.builder().id(1L).build()));
        assertNotNull(clienteService.obtenerClienteDetalle(DetalleClienteRequest.builder().curp("HEGC900101HDFRRN09").build()));
        assertNotNull(clienteService.obtenerClienteDetalle(DetalleClienteRequest.builder().rfc("HEGC900101XYZ").build()));
        assertNotNull(clienteService.obtenerClienteDetalle(DetalleClienteRequest.builder().numeroCuenta("5551234567").build()));

        assertThrows(com.proyecto.servicios.exception.onboarding.ValidacionNegocioException.class,
                () -> clienteService.obtenerClienteDetalle(DetalleClienteRequest.builder().build()));
    }

    @Test
    @DisplayName("Debe obtener cliente por correo exitosamente")
    void testObtenerClientePorCorreo() {
        when(clienteRepository.findByCorreo("carlos.h@example.com")).thenReturn(Optional.of(clienteExistente));
        ClienteResponse resp = clienteService.obtenerClientePorCorreo("carlos.h@example.com");
        assertNotNull(resp);
        assertEquals("Carlos", resp.getNombre());
    }

    @Test
    @DisplayName("Debe actualizar campos personales y domicilio del cliente")
    void testActualizarClienteCamposExitosos() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        com.proyecto.servicios.dto.onboarding.DomicilioDto domDto = new com.proyecto.servicios.dto.onboarding.DomicilioDto();
        domDto.setCalle("Av Reforma");
        domDto.setNumeroExterior("123");
        domDto.setColonia("Centro");
        domDto.setMunicipio("Cuauhtémoc");
        domDto.setEstado("CDMX");
        domDto.setCodigoPostal("06000");
        domDto.setPais("MEX");

        ClienteUpdateRequest updateRequest = new ClienteUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setNombre("Carlos Modificado");
        updateRequest.setSegundoNombre("Alberto");
        updateRequest.setApellidoPaterno("Hernández");
        updateRequest.setApellidoMaterno("Gómez");
        updateRequest.setFechaNacimiento(LocalDate.of(1992, 5, 10));
        updateRequest.setSexo("M");
        updateRequest.setNacionalidad("MEX");
        updateRequest.setEstadoCivil("CASADO");
        updateRequest.setTelefonoMovil("5512345678");
        updateRequest.setTelefonoAlternativo("5587654321");
        updateRequest.setOcupacion("Ingeniero");
        updateRequest.setEmpresa("Tech Corp");
        updateRequest.setIngresoMensual(new BigDecimal("35000.00"));
        updateRequest.setDomicilio(domDto);

        ClienteResponse response = clienteService.actualizarClienteParcial(updateRequest);

        assertNotNull(response);
        assertEquals("Carlos Modificado", response.getNombre());
        assertEquals("Alberto", response.getSegundoNombre());
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe rechazar cambio si la fecha de nacimiento no cumple mayoria de edad")
    void testActualizarClienteMenorDeEdad() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente));

        ClienteUpdateRequest updateRequest = new ClienteUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setFechaNacimiento(LocalDate.now().minusYears(15));

        assertThrows(com.proyecto.servicios.exception.onboarding.ValidacionNegocioException.class,
                () -> clienteService.actualizarClienteParcial(updateRequest));
    }

    @Test
    @DisplayName("Debe rechazar modificación de correo si ya existe registrado")
    void testActualizarClienteCorreoDuplicado() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente));
        when(clienteRepository.existsByCorreo("duplicado@example.com")).thenReturn(true);

        ClienteUpdateRequest updateRequest = new ClienteUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setCorreo("duplicado@example.com");

        assertThrows(com.proyecto.servicios.exception.onboarding.CorreoDuplicadoException.class,
                () -> clienteService.actualizarClienteParcial(updateRequest));
    }

    @Test
    @DisplayName("Debe actualizar correo correctamente en cliente y su usuario")
    void testActualizarClienteCorreoExitoso() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente));
        when(clienteRepository.existsByCorreo("nuevo@example.com")).thenReturn(false);
        when(usuarioRepository.existsByCorreo("nuevo@example.com")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        ClienteUpdateRequest updateRequest = new ClienteUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setCorreo("nuevo@example.com");

        ClienteResponse response = clienteService.actualizarClienteParcial(updateRequest);

        assertNotNull(response);
        assertEquals("nuevo@example.com", clienteExistente.getCorreo());
        assertEquals("nuevo@example.com", clienteExistente.getUsuario().getCorreo());
    }
}
