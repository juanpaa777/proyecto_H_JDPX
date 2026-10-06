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
     * Operación 100% atómica.
     */
    @Transactional(rollbackFor = Exception.class)
    public ClienteResponse registrarCliente(RegistroClienteRequest request) {
        log.info("Iniciando onboarding para cliente con CURP: {}, RFC: {}, Correo: {}",
                request.getCurp(), request.getRfc(), request.getCorreo());

        // 1. Validaciones de Negocio Obligatorias
        validarUnicidadYReglas(request);

        // 2. Construir entidad Cliente
        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre().trim());
        cliente.setSegundoNombre(request.getSegundoNombre() != null ? request.getSegundoNombre().trim() : null);
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
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo() != null && !request.getTelefonoAlternativo().trim().isEmpty()
                ? request.getTelefonoAlternativo().trim() : null);
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
        domicilio.setNumeroInterior(domDto.getNumeroInterior() != null ? domDto.getNumeroInterior().trim() : null);
        domicilio.setColonia(domDto.getColonia().trim());
        domicilio.setMunicipio(domDto.getMunicipio().trim());
        domicilio.setEstado(domDto.getEstado().trim());
        domicilio.setCodigoPostal(domDto.getCodigoPostal().trim());
        domicilio.setPais(domDto.getPais() != null ? domDto.getPais().trim() : "México");

        domicilio = domicilioRepository.save(domicilio);
        cliente.setDomicilio(domicilio);

        // 4. Crear Cuenta bancaria automática activa con saldo inicial
        BigDecimal saldoInicial = (request.getSaldoInicial() != null)
                ? request.getSaldoInicial()
                : saldoInicialDefault;

        if (saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionNegocioException("El saldo inicial no puede ser negativo");
        }

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
        // Validación de mayoría de edad
        LocalDate hoy = LocalDate.now();
        if (request.getFechaNacimiento().isAfter(hoy)) {
            throw new ValidacionNegocioException("La fecha de nacimiento no puede ser una fecha futura");
        }
        if (Period.between(request.getFechaNacimiento(), hoy).getYears() < 18) {
            throw new ValidacionNegocioException("El cliente debe ser mayor de edad (18 años o más)");
        }

        // Unicidad de CURP
        if (clienteRepository.existsByCurp(request.getCurp().trim().toUpperCase())) {
            throw new CurpDuplicadaException(request.getCurp().trim().toUpperCase());
        }

        // Unicidad de RFC
        if (clienteRepository.existsByRfc(request.getRfc().trim().toUpperCase())) {
            throw new RfcDuplicadoException(request.getRfc().trim().toUpperCase());
        }

        // Unicidad de Correo
        String correoNormalizado = request.getCorreo().trim().toLowerCase();
        if (clienteRepository.existsByCorreo(correoNormalizado) || usuarioRepository.existsByCorreo(correoNormalizado)) {
            throw new CorreoDuplicadoException(correoNormalizado);
        }
    }
}
