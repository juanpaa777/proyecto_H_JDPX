package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.*;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.exception.onboarding.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.onboarding.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.mapper.onboarding.ClienteMapper;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final GeneradorNumeroCuentaService generadorNumeroCuentaService;

    @Value("${banco.onboarding.saldo-inicial-default:1000.00}")
    private BigDecimal saldoInicialDefault;

    @Transactional(readOnly = true)
    public CuentaResponse obtenerCuentaDetalle(DetalleCuentaRequest request) {
        return obtenerCuentaPorNumero(request.getNumeroCuenta());
    }

    @Transactional(readOnly = true)
    public CuentaResponse obtenerCuentaPorNumero(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .map(ClienteMapper::toCuentaResponse)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
    }

    @Transactional(readOnly = true)
    public SaldoResponse obtenerSaldoDetalle(DetalleCuentaRequest request) {
        return obtenerSaldoCuenta(request.getNumeroCuenta());
    }

    @Transactional(readOnly = true)
    public SaldoResponse obtenerSaldoCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
        return new SaldoResponse(cuenta.getNumeroCuenta(), cuenta.getSaldo(), cuenta.getEstatus());
    }

    @Transactional(readOnly = true)
    public Page<CuentaResponse> listarCuentasPaginadas(ConsultaCuentaRequest request) {
        int page = Optional.ofNullable(request.getPage()).orElse(0);
        int size = Optional.ofNullable(request.getSize()).orElse(10);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        return Optional.ofNullable(request.getClienteId())
                .map(id -> Optional.ofNullable(request.getEstatus())
                        .filter(Predicate.not(String::isBlank))
                        .map(est -> cuentaRepository.findByClienteIdAndEstatusIgnoreCase(id, est.trim(), pageable))
                        .orElseGet(() -> cuentaRepository.findByClienteId(id, pageable)))
                .orElseGet(() -> Optional.ofNullable(request.getEstatus())
                        .filter(Predicate.not(String::isBlank))
                        .map(est -> cuentaRepository.findByEstatusIgnoreCase(est.trim(), pageable))
                        .orElseGet(() -> cuentaRepository.findAll(pageable)))
                .map(ClienteMapper::toCuentaResponse);
    }

    /**
     * Crear una cuenta adicional asociada a un cliente.
     * Cero sentencias if.
     */
    @Transactional(rollbackFor = Exception.class)
    public CuentaResponse crearCuentaBancaria(CuentaCreateRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(request.getClienteId()));

        Optional.of(cliente)
                .filter(c -> !Boolean.TRUE.equals(c.getActivo()))
                .ifPresent(c -> { throw new ValidacionNegocioException("No se pueden crear cuentas para un cliente inactivo"); });

        BigDecimal saldo = Optional.ofNullable(request.getSaldoInicial())
                .orElse(saldoInicialDefault);

        Optional.of(saldo)
                .filter(s -> s.compareTo(BigDecimal.ZERO) < 0)
                .ifPresent(s -> { throw new ValidacionNegocioException("El saldo inicial no puede ser negativo"); });

        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(generadorNumeroCuentaService.generarNumeroCuentaUnico());
        cuenta.setSaldo(saldo);
        cuenta.setEstatus("ACTIVA");

        Cuenta cuentaGuardada = cuentaRepository.save(cuenta);
        log.info("Nueva cuenta bancaria creada: {} para cliente ID: {}", cuentaGuardada.getNumeroCuenta(), cliente.getId());
        return ClienteMapper.toCuentaResponse(cuentaGuardada);
    }

    /**
     * Actualizar parcialmente la información de una cuenta mediante RequestBody.
     * Cero sentencias if.
     */
    @Transactional(rollbackFor = Exception.class)
    public CuentaResponse actualizarCuentaParcial(CuentaUpdateRequest request) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(request.getNumeroCuenta().trim())
                .orElseThrow(() -> new CuentaNoEncontradaException(request.getNumeroCuenta()));

        // Validar y aplicar saldo sin IF
        Optional.ofNullable(request.getSaldo())
                .filter(s -> s.compareTo(BigDecimal.ZERO) < 0)
                .ifPresent(s -> { throw new ValidacionNegocioException("El saldo de la cuenta no puede ser negativo"); });

        Optional.ofNullable(request.getSaldo())
                .ifPresent(cuenta::setSaldo);

        // Validar y aplicar estatus sin IF
        Optional.ofNullable(request.getEstatus())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .map(String::toUpperCase)
                .ifPresent(nuevoEstatus -> {
                    Optional.of(nuevoEstatus)
                            .filter(e -> "ACTIVA".equals(e) && !Boolean.TRUE.equals(cuenta.getCliente().getActivo()))
                            .ifPresent(e -> { throw new ValidacionNegocioException("No se puede activar una cuenta de un cliente que está inactivo"); });

                    cuenta.setEstatus(nuevoEstatus);
                });

        Cuenta cuentaActualizada = cuentaRepository.save(cuenta);
        log.info("Cuenta bancaria {} actualizada satisfactoriamente", cuentaActualizada.getNumeroCuenta());
        return ClienteMapper.toCuentaResponse(cuentaActualizada);
    }
}
