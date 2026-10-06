package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.CuentaCreateRequest;
import com.proyecto.servicios.dto.onboarding.CuentaResponse;
import com.proyecto.servicios.dto.onboarding.CuentaUpdateRequest;
import com.proyecto.servicios.dto.onboarding.SaldoResponse;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.exception.onboarding.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.onboarding.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.onboarding.ModificacionNoPermitidaException;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.mapper.onboarding.ClienteMapper;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
    public CuentaResponse consultarPorNumeroCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
        return ClienteMapper.toCuentaResponse(cuenta);
    }

    @Transactional(readOnly = true)
    public SaldoResponse consultarSaldo(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
        return new SaldoResponse(cuenta.getNumeroCuenta(), cuenta.getSaldo(), cuenta.getEstatus());
    }

    @Transactional(readOnly = true)
    public List<CuentaResponse> consultarPorClienteId(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ClienteNoEncontradoException(clienteId);
        }
        return cuentaRepository.findByClienteId(clienteId).stream()
                .map(ClienteMapper::toCuentaResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CuentaResponse> consultarPorEstatus(String estatus) {
        return cuentaRepository.findByEstatusIgnoreCase(estatus.trim()).stream()
                .map(ClienteMapper::toCuentaResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CuentaResponse> consultarCuentasActivas() {
        return consultarPorEstatus("ACTIVA");
    }

    /**
     * Crear una cuenta adicional asociada a un cliente.
     * Regla de negocio: Solo los clientes activos podrán tener cuentas activas.
     */
    @Transactional(rollbackFor = Exception.class)
    public CuentaResponse crearCuenta(CuentaCreateRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(request.getClienteId()));

        if (!cliente.getActivo()) {
            throw new ValidacionNegocioException("No se pueden crear cuentas para un cliente inactivo");
        }

        BigDecimal saldo = (request.getSaldoInicial() != null)
                ? request.getSaldoInicial()
                : saldoInicialDefault;

        if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionNegocioException("El saldo inicial no puede ser negativo");
        }

        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(generadorNumeroCuentaService.generarNumeroCuentaUnico());
        cuenta.setSaldo(saldo);
        cuenta.setEstatus("ACTIVA");

        cuenta = cuentaRepository.save(cuenta);
        log.info("Nueva cuenta bancaria creada: {} para cliente ID: {}", cuenta.getNumeroCuenta(), cliente.getId());
        return ClienteMapper.toCuentaResponse(cuenta);
    }

    /**
     * Actualizar parcialmente la información de una cuenta.
     * Regla de negocio: No se permite modificar el número de cuenta.
     */
    @Transactional(rollbackFor = Exception.class)
    public CuentaResponse actualizarParcial(String numeroCuenta, CuentaUpdateRequest request) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));

        // Validar inmutabilidad de número de cuenta
        if (request.getNumeroCuenta() != null && !request.getNumeroCuenta().trim().equalsIgnoreCase(cuenta.getNumeroCuenta())) {
            throw new ModificacionNoPermitidaException("No está permitido modificar el número de cuenta bancaria");
        }

        if (request.getSaldo() != null) {
            if (request.getSaldo().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidacionNegocioException("El saldo de la cuenta no puede ser negativo");
            }
            cuenta.setSaldo(request.getSaldo());
        }

        if (request.getEstatus() != null && !request.getEstatus().trim().isEmpty()) {
            String nuevoEstatus = request.getEstatus().trim().toUpperCase();
            if ("ACTIVA".equals(nuevoEstatus) && !cuenta.getCliente().getActivo()) {
                throw new ValidacionNegocioException("No se puede activar una cuenta de un cliente que está inactivo");
            }
            cuenta.setEstatus(nuevoEstatus);
        }

        cuenta = cuentaRepository.save(cuenta);
        log.info("Cuenta bancaria {} actualizada satisfactoriamente", cuenta.getNumeroCuenta());
        return ClienteMapper.toCuentaResponse(cuenta);
    }
}
