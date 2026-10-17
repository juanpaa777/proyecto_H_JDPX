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
import com.proyecto.servicios.mapper.onboarding.ClienteMapper;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.DomicilioRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final ClienteRepository clienteRepository;
    private final DomicilioRepository domicilioRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeneradorNumeroCuentaService generadorNumeroCuentaService;
    private final PasswordEncoder passwordEncoder;

    @Value("${banco.onboarding.saldo-inicial-default:1000.00}")
    private BigDecimal saldoInicialDefault;

    /**
     * Registro completo e integral de un cliente persona física.
     * Operación 100% atómica y funcional con cero sentencias IF.
     */
    @Transactional(rollbackFor = Exception.class)
    public ClienteResponse registrarNuevoCliente(RegistroClienteRequest request) {
        log.info("Iniciando onboarding para cliente con CURP: {}, RFC: {}, Correo: {}",
                request.getCurp(), request.getRfc(), request.getCorreo());

        // 1. Validaciones funcionales sin IF
        validarUnicidadYReglas(request);

        // 2. Construir entidad Cliente
        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre().trim());
        cliente.setSegundoNombre(Optional.ofNullable(request.getSegundoNombre())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .orElse(null));
        cliente.setApellidoPaterno(request.getApellidoPaterno().trim());
        cliente.setApellidoMaterno(request.getApellidoMaterno().trim());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setCurp(request.getCurp().trim().toUpperCase());
        cliente.setRfc(request.getRfc().trim().toUpperCase());
        cliente.setSexo(request.getSexo().trim().toUpperCase());
        cliente.setNacionalidad(request.getNacionalidad().trim());
        cliente.setEstadoCivil(request.getEstadoCivil().trim());
        cliente.setCorreo(request.getCorreo().trim().toLowerCase());
        cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        cliente.setTelefonoAlternativo(Optional.ofNullable(request.getTelefonoAlternativo())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .orElse(null));
        cliente.setOcupacion(request.getOcupacion().trim());
        cliente.setEmpresa(request.getEmpresa().trim());
        cliente.setIngresoMensual(request.getIngresoMensual());
        cliente.setActivo(true);

        cliente = clienteRepository.save(cliente);

        // 3. Crear Domicilio asociado
        DomicilioDto domDto = request.getDomicilio();
        Domicilio domicilio = new Domicilio();
        domicilio.setCliente(cliente);
        domicilio.setCalle(domDto.getCalle().trim());
        domicilio.setNumeroExterior(domDto.getNumeroExterior().trim());
        domicilio.setNumeroInterior(Optional.ofNullable(domDto.getNumeroInterior())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .orElse(null));
        domicilio.setColonia(domDto.getColonia().trim());
        domicilio.setMunicipio(domDto.getMunicipio().trim());
        domicilio.setEstado(domDto.getEstado().trim());
        domicilio.setCodigoPostal(domDto.getCodigoPostal().trim());
        domicilio.setPais(Optional.ofNullable(domDto.getPais())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .orElse("México"));

        domicilio = domicilioRepository.save(domicilio);
        cliente.setDomicilio(domicilio);

        // 4. Crear Cuenta bancaria automática activa con saldo inicial
        BigDecimal saldoInicial = Optional.ofNullable(request.getSaldoInicial())
                .orElse(saldoInicialDefault);

        Optional.of(saldoInicial)
                .filter(s -> s.compareTo(BigDecimal.ZERO) < 0)
                .ifPresent(s -> { throw new ValidacionNegocioException("El saldo inicial no puede ser negativo"); });

        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(generadorNumeroCuentaService.generarNumeroCuentaUnico());
        cuenta.setSaldo(saldoInicial);
        cuenta.setEstatus("ACTIVA");

        cuenta = cuentaRepository.save(cuenta);
        cliente.getCuentas().add(cuenta);

        // 5. Crear Usuario de acceso asociado con contraseña cifrada en BCrypt
        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreo(cliente.getCorreo());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);

        usuario = usuarioRepository.save(usuario);
        cliente.setUsuario(usuario);

        log.info("Onboarding completado exitosamente. Cliente ID: {}, Cuenta: {}, Usuario: {}",
                cliente.getId(), cuenta.getNumeroCuenta(), usuario.getCorreo());

        return ClienteMapper.toResponse(cliente);
    }

    private void validarUnicidadYReglas(RegistroClienteRequest request) {
        // Validación de mayoría de edad (Cero IF)
        Optional.ofNullable(request.getFechaNacimiento())
                .filter(f -> f.isAfter(LocalDate.now()))
                .ifPresent(f -> { throw new ValidacionNegocioException("La fecha de nacimiento no puede ser una fecha futura"); });

        Optional.ofNullable(request.getFechaNacimiento())
                .filter(f -> Period.between(f, LocalDate.now()).getYears() < 18)
                .ifPresent(f -> { throw new ValidacionNegocioException("El cliente debe ser mayor de edad (18 años o más)"); });

        // Unicidad de CURP (Cero IF)
        Optional.of(request.getCurp().trim().toUpperCase())
                .filter(clienteRepository::existsByCurp)
                .ifPresent(c -> { throw new CurpDuplicadaException(c); });

        // Unicidad de RFC (Cero IF)
        Optional.of(request.getRfc().trim().toUpperCase())
                .filter(clienteRepository::existsByRfc)
                .ifPresent(r -> { throw new RfcDuplicadoException(r); });

        // Unicidad de Correo (Cero IF)
        Optional.of(request.getCorreo().trim().toLowerCase())
                .filter(c -> clienteRepository.existsByCorreo(c) || usuarioRepository.existsByCorreo(c))
                .ifPresent(c -> { throw new CorreoDuplicadoException(c); });
    }
}
