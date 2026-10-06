package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class GeneradorNumeroCuentaService {

    private final CuentaRepository cuentaRepository;
    private final SecureRandom random = new SecureRandom();

    /**
     * Genera un número de cuenta bancaria único de 10 dígitos.
     * Ejemplo: "4028193847"
     */
    public String generarNumeroCuentaUnico() {
        String numeroCuenta;
        do {
            long number = 1000000000L + (long)(random.nextDouble() * 9000000000L);
            numeroCuenta = String.valueOf(number);
        } while (cuentaRepository.existsByNumeroCuenta(numeroCuenta));
        return numeroCuenta;
    }
}
