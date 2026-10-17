package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.*;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.exception.onboarding.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.onboarding.CorreoDuplicadoException;
import com.proyecto.servicios.exception.onboarding.ModificacionNoPermitidaException;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.mapper.onboarding.ClienteMapper;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.repositorys.onboarding.specification.ClienteSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Page<ClienteResponse> buscarClientesPaginados(ConsultaClienteRequest request) {
        int page = Optional.ofNullable(request.getPage()).orElse(0);
        int size = Optional.ofNullable(request.getSize()).orElse(10);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Specification<Cliente> spec = ClienteSpecification.conFiltros(request);
        return clienteRepository.findAll(spec, pageable).map(ClienteMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerClienteDetalle(DetalleClienteRequest request) {
        return Optional.ofNullable(request.getId())
                .map(this::obtenerClientePorId)
                .or(() -> Optional.ofNullable(request.getCurp())
                        .filter(Predicate.not(String::isBlank))
                        .map(this::obtenerClientePorCurp))
                .or(() -> Optional.ofNullable(request.getRfc())
                        .filter(Predicate.not(String::isBlank))
                        .map(this::obtenerClientePorRfc))
                .or(() -> Optional.ofNullable(request.getNumeroCuenta())
                        .filter(Predicate.not(String::isBlank))
                        .map(this::obtenerClientePorNumeroCuenta))
                .orElseThrow(() -> new ValidacionNegocioException("Debe especificar id, curp, rfc o numeroCuenta para consultar el detalle"));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerClientePorId(Long id) {
        return clienteRepository.findById(id)
                .map(ClienteMapper::toResponse)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerClientePorCurp(String curp) {
        return clienteRepository.findByCurp(curp.trim().toUpperCase())
                .map(ClienteMapper::toResponse)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente con la CURP: " + curp));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerClientePorRfc(String rfc) {
        return clienteRepository.findByRfc(rfc.trim().toUpperCase())
                .map(ClienteMapper::toResponse)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente con el RFC: " + rfc));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerClientePorCorreo(String correo) {
        return clienteRepository.findByCorreo(correo.trim().toLowerCase())
                .map(ClienteMapper::toResponse)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente con el correo: " + correo));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerClientePorNumeroCuenta(String numeroCuenta) {
        return clienteRepository.findByNumeroCuenta(numeroCuenta.trim())
                .map(ClienteMapper::toResponse)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente titular de la cuenta: " + numeroCuenta));
    }

    /**
     * Actualización parcial de información utilizando exclusivamente programación funcional.
     * Cero sentencias if, validaciones de inmutabilidad y reglas de negocio declarativas.
     */
    @Transactional(rollbackFor = Exception.class)
    public ClienteResponse actualizarClienteParcial(ClienteUpdateRequest request) {
        Cliente cliente = clienteRepository.findById(request.getId())
                .orElseThrow(() -> new ClienteNoEncontradoException(request.getId()));

        // 1. Validar reglas de inmutabilidad (Cero IF)
        Optional.ofNullable(request.getCurp())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .filter(c -> !c.equalsIgnoreCase(cliente.getCurp()))
                .ifPresent(c -> { throw new ModificacionNoPermitidaException("No está permitido modificar la CURP de un cliente ya registrado"); });

        Optional.ofNullable(request.getRfc())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .filter(r -> !r.equalsIgnoreCase(cliente.getRfc()))
                .ifPresent(r -> { throw new ModificacionNoPermitidaException("No está permitido modificar el RFC de un cliente ya registrado"); });

        Optional.ofNullable(request.getNumeroCuenta())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .ifPresent(nc -> { throw new ModificacionNoPermitidaException("No está permitido modificar el número de cuenta bancaria"); });

        // 2. Modificaciones personales permitidas
        Optional.ofNullable(request.getNombre())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .ifPresent(cliente::setNombre);

        Optional.ofNullable(request.getSegundoNombre())
                .map(String::trim)
                .map(s -> s.isEmpty() ? null : s)
                .ifPresent(cliente::setSegundoNombre);

        Optional.ofNullable(request.getApellidoPaterno())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .ifPresent(cliente::setApellidoPaterno);

        Optional.ofNullable(request.getApellidoMaterno())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .ifPresent(cliente::setApellidoMaterno);

        // Validación de mayoría de edad sin IF
        Optional.ofNullable(request.getFechaNacimiento())
                .filter(f -> f.isAfter(LocalDate.now()) || Period.between(f, LocalDate.now()).getYears() < 18)
                .ifPresent(f -> { throw new ValidacionNegocioException("La fecha de nacimiento modificada debe cumplir con la mayoría de edad (18 años o más)"); });

        Optional.ofNullable(request.getFechaNacimiento())
                .ifPresent(cliente::setFechaNacimiento);

        Optional.ofNullable(request.getSexo())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .map(String::toUpperCase)
                .ifPresent(cliente::setSexo);

        Optional.ofNullable(request.getNacionalidad())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .ifPresent(cliente::setNacionalidad);

        Optional.ofNullable(request.getEstadoCivil())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .ifPresent(cliente::setEstadoCivil);

        // 3. Modificaciones de contacto y unicidad de correo
        Optional.ofNullable(request.getCorreo())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .map(String::toLowerCase)
                .filter(nuevoCorreo -> !nuevoCorreo.equalsIgnoreCase(cliente.getCorreo()))
                .ifPresent(nuevoCorreo -> {
                    Optional.of(nuevoCorreo)
                            .filter(c -> clienteRepository.existsByCorreo(c) || usuarioRepository.existsByCorreo(c))
                            .ifPresent(c -> { throw new CorreoDuplicadoException(c); });

                    cliente.setCorreo(nuevoCorreo);
                    Optional.ofNullable(cliente.getUsuario()).ifPresent(u -> u.setCorreo(nuevoCorreo));
                });

        Optional.ofNullable(request.getTelefonoMovil())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .ifPresent(cliente::setTelefonoMovil);

        Optional.ofNullable(request.getTelefonoAlternativo())
                .map(String::trim)
                .map(t -> t.isEmpty() ? null : t)
                .ifPresent(cliente::setTelefonoAlternativo);

        // 4. Modificaciones laborales
        Optional.ofNullable(request.getOcupacion())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .ifPresent(cliente::setOcupacion);

        Optional.ofNullable(request.getEmpresa())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .ifPresent(cliente::setEmpresa);

        Optional.ofNullable(request.getIngresoMensual())
                .ifPresent(cliente::setIngresoMensual);

        // 5. Modificaciones de domicilio
        Optional.ofNullable(request.getDomicilio()).ifPresent(domDto -> {
            Domicilio dom = Optional.ofNullable(cliente.getDomicilio()).orElseGet(() -> {
                Domicilio nuevo = new Domicilio();
                nuevo.setCliente(cliente);
                cliente.setDomicilio(nuevo);
                return nuevo;
            });

            Optional.ofNullable(domDto.getCalle()).map(String::trim).ifPresent(dom::setCalle);
            Optional.ofNullable(domDto.getNumeroExterior()).map(String::trim).ifPresent(dom::setNumeroExterior);
            Optional.ofNullable(domDto.getNumeroInterior()).map(String::trim).ifPresent(dom::setNumeroInterior);
            Optional.ofNullable(domDto.getColonia()).map(String::trim).ifPresent(dom::setColonia);
            Optional.ofNullable(domDto.getMunicipio()).map(String::trim).ifPresent(dom::setMunicipio);
            Optional.ofNullable(domDto.getEstado()).map(String::trim).ifPresent(dom::setEstado);
            Optional.ofNullable(domDto.getCodigoPostal()).map(String::trim).ifPresent(dom::setCodigoPostal);
            Optional.ofNullable(domDto.getPais()).map(String::trim).ifPresent(dom::setPais);
        });

        Cliente clienteGuardado = clienteRepository.save(cliente);
        log.info("Cliente ID: {} actualizado satisfactoriamente", clienteGuardado.getId());
        return ClienteMapper.toResponse(clienteGuardado);
    }

    /**
     * Baja lógica del cliente, cuentas y usuario en cascada mediante DTO en RequestBody.
     * Cero IF en su ejecución.
     */
    @Transactional(rollbackFor = Exception.class)
    public ClienteResponse darDeBajaClienteLogica(EliminaClienteRequest request) {
        Cliente cliente = clienteRepository.findById(request.getId())
                .orElseThrow(() -> new ClienteNoEncontradoException(request.getId()));

        cliente.setActivo(false);

        Optional.ofNullable(cliente.getUsuario()).ifPresent(u -> {
            u.setActivo(false);
            usuarioRepository.save(u);
        });

        Optional.ofNullable(cliente.getCuentas()).ifPresent(cuentas ->
                cuentas.forEach(cuenta -> {
                    cuenta.setEstatus("INACTIVA");
                    cuentaRepository.save(cuenta);
                })
        );

        Cliente clienteDesactivado = clienteRepository.save(cliente);
        log.info("Baja lógica aplicada al cliente ID: {}. Cuentas y usuario desactivados.", request.getId());
        return ClienteMapper.toResponse(clienteDesactivado);
    }
}
