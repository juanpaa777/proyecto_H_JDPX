package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.ClienteResponse;
import com.proyecto.servicios.dto.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.dto.onboarding.DomicilioDto;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.onboarding.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.onboarding.CorreoDuplicadoException;
import com.proyecto.servicios.exception.onboarding.ModificacionNoPermitidaException;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.mapper.onboarding.ClienteMapper;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<ClienteResponse> consultarTodos() {
        return clienteRepository.findAll().stream()
                .map(ClienteMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClienteResponse consultarPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));
        return ClienteMapper.toResponse(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponse consultarPorCurp(String curp) {
        Cliente cliente = clienteRepository.findByCurp(curp.trim().toUpperCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente con la CURP: " + curp));
        return ClienteMapper.toResponse(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponse consultarPorRfc(String rfc) {
        Cliente cliente = clienteRepository.findByRfc(rfc.trim().toUpperCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente con el RFC: " + rfc));
        return ClienteMapper.toResponse(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponse consultarPorCorreo(String correo) {
        Cliente cliente = clienteRepository.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente con el correo: " + correo));
        return ClienteMapper.toResponse(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponse consultarPorNumeroCuenta(String numeroCuenta) {
        Cliente cliente = clienteRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente titular de la cuenta: " + numeroCuenta));
        return ClienteMapper.toResponse(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarPorNombre(String nombre) {
        return clienteRepository.findByNombreContainingIgnoreCase(nombre).stream()
                .map(ClienteMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarPorApellidoPaterno(String apellidoPaterno) {
        return clienteRepository.findByApellidoPaternoContainingIgnoreCase(apellidoPaterno).stream()
                .map(com.proyecto.servicios.mapper.onboarding.ClienteMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarPorApellidoMaterno(String apellidoMaterno) {
        return clienteRepository.findByApellidoMaternoContainingIgnoreCase(apellidoMaterno).stream()
                .map(com.proyecto.servicios.mapper.onboarding.ClienteMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> consultarActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(com.proyecto.servicios.mapper.onboarding.ClienteMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> consultarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        return clienteRepository.findByFechaCreacionBetween(inicio, fin).stream()
                .map(com.proyecto.servicios.mapper.onboarding.ClienteMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Actualización parcial de información.
     * Regla de negocio estricta: NO se permite modificar CURP, RFC ni Número de cuenta.
     */
    @Transactional(rollbackFor = Exception.class)
    public ClienteResponse actualizarParcial(Long id, ClienteUpdateRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));

        // 1. Validar reglas de inmutabilidad
        if (request.getCurp() != null && !request.getCurp().trim().equalsIgnoreCase(cliente.getCurp())) {
            throw new ModificacionNoPermitidaException("No está permitido modificar la CURP de un cliente ya registrado");
        }

        if (request.getRfc() != null && !request.getRfc().trim().equalsIgnoreCase(cliente.getRfc())) {
            throw new ModificacionNoPermitidaException("No está permitido modificar el RFC de un cliente ya registrado");
        }

        if (request.getNumeroCuenta() != null && !request.getNumeroCuenta().trim().isEmpty()) {
            throw new ModificacionNoPermitidaException("No está permitido modificar el número de cuenta bancaria");
        }

        // 2. Modificaciones personales permitidas
        if (request.getNombre() != null && !request.getNombre().trim().isEmpty()) {
            cliente.setNombre(request.getNombre().trim());
        }
        if (request.getSegundoNombre() != null) {
            cliente.setSegundoNombre(request.getSegundoNombre().trim().isEmpty() ? null : request.getSegundoNombre().trim());
        }
        if (request.getApellidoPaterno() != null && !request.getApellidoPaterno().trim().isEmpty()) {
            cliente.setApellidoPaterno(request.getApellidoPaterno().trim());
        }
        if (request.getApellidoMaterno() != null && !request.getApellidoMaterno().trim().isEmpty()) {
            cliente.setApellidoMaterno(request.getApellidoMaterno().trim());
        }
        if (request.getFechaNacimiento() != null) {
            LocalDate hoy = LocalDate.now();
            if (request.getFechaNacimiento().isAfter(hoy) || Period.between(request.getFechaNacimiento(), hoy).getYears() < 18) {
                throw new ValidacionNegocioException("La fecha de nacimiento modificada debe cumplir con la mayoría de edad (18 años o más)");
            }
            cliente.setFechaNacimiento(request.getFechaNacimiento());
        }
        if (request.getSexo() != null && !request.getSexo().trim().isEmpty()) {
            cliente.setSexo(request.getSexo().trim().toUpperCase());
        }
        if (request.getNacionalidad() != null && !request.getNacionalidad().trim().isEmpty()) {
            cliente.setNacionalidad(request.getNacionalidad().trim());
        }
        if (request.getEstadoCivil() != null && !request.getEstadoCivil().trim().isEmpty()) {
            cliente.setEstadoCivil(request.getEstadoCivil().trim());
        }

        // 3. Modificaciones de contacto
        if (request.getCorreo() != null && !request.getCorreo().trim().isEmpty()) {
            String nuevoCorreo = request.getCorreo().trim().toLowerCase();
            if (!nuevoCorreo.equalsIgnoreCase(cliente.getCorreo())) {
                if (clienteRepository.existsByCorreo(nuevoCorreo) || usuarioRepository.existsByCorreo(nuevoCorreo)) {
                    throw new CorreoDuplicadoException(nuevoCorreo);
                }
                cliente.setCorreo(nuevoCorreo);
                // Si tiene usuario asociado, actualizar su login (correo)
                if (cliente.getUsuario() != null) {
                    cliente.getUsuario().setCorreo(nuevoCorreo);
                }
            }
        }
        if (request.getTelefonoMovil() != null && !request.getTelefonoMovil().trim().isEmpty()) {
            cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        }
        if (request.getTelefonoAlternativo() != null) {
            cliente.setTelefonoAlternativo(request.getTelefonoAlternativo().trim().isEmpty() ? null : request.getTelefonoAlternativo().trim());
        }

        // 4. Modificaciones laborales
        if (request.getOcupacion() != null && !request.getOcupacion().trim().isEmpty()) {
            cliente.setOcupacion(request.getOcupacion().trim());
        }
        if (request.getEmpresa() != null && !request.getEmpresa().trim().isEmpty()) {
            cliente.setEmpresa(request.getEmpresa().trim());
        }
        if (request.getIngresoMensual() != null) {
            cliente.setIngresoMensual(request.getIngresoMensual());
        }

        // 5. Modificaciones de domicilio
        if (request.getDomicilio() != null) {
            DomicilioDto domDto = request.getDomicilio();
            Domicilio dom = cliente.getDomicilio();
            if (dom == null) {
                dom = new Domicilio();
                dom.setCliente(cliente);
                cliente.setDomicilio(dom);
            }
            if (domDto.getCalle() != null) dom.setCalle(domDto.getCalle().trim());
            if (domDto.getNumeroExterior() != null) dom.setNumeroExterior(domDto.getNumeroExterior().trim());
            if (domDto.getNumeroInterior() != null) dom.setNumeroInterior(domDto.getNumeroInterior().trim());
            if (domDto.getColonia() != null) dom.setColonia(domDto.getColonia().trim());
            if (domDto.getMunicipio() != null) dom.setMunicipio(domDto.getMunicipio().trim());
            if (domDto.getEstado() != null) dom.setEstado(domDto.getEstado().trim());
            if (domDto.getCodigoPostal() != null) dom.setCodigoPostal(domDto.getCodigoPostal().trim());
            if (domDto.getPais() != null) dom.setPais(domDto.getPais().trim());
        }

        cliente = clienteRepository.save(cliente);
        log.info("Cliente ID: {} actualizado satisfactoriamente", cliente.getId());
        return com.proyecto.servicios.mapper.onboarding.ClienteMapper.toResponse(cliente);
    }

    /**
     * Baja lógica del cliente.
     * Regla: Desactivar cliente sin eliminar físicamente.
     * Regla en cascada: Su usuario y sus cuentas quedan inactivas automáticamente.
     */
    @Transactional(rollbackFor = Exception.class)
    public ClienteResponse bajaLogica(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));

        cliente.setActivo(false);

        // Desactivar usuario asociado
        if (cliente.getUsuario() != null) {
            cliente.getUsuario().setActivo(false);
            usuarioRepository.save(cliente.getUsuario());
        }

        // Desactivar cuentas bancarias asociadas ("Solo los clientes activos podrán tener cuentas activas")
        if (cliente.getCuentas() != null) {
            for (Cuenta cuenta : cliente.getCuentas()) {
                cuenta.setEstatus("INACTIVA");
                cuentaRepository.save(cuenta);
            }
        }

        cliente = clienteRepository.save(cliente);
        log.info("Baja lógica aplicada al cliente ID: {}. Cuentas y usuario desactivados.", id);
        return com.proyecto.servicios.mapper.onboarding.ClienteMapper.toResponse(cliente);
    }
}
