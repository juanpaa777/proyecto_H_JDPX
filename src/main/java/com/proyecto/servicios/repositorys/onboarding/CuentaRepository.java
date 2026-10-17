package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    boolean existsByNumeroCuenta(String numeroCuenta);

    Page<Cuenta> findByClienteId(Long clienteId, Pageable pageable);

    Page<Cuenta> findByEstatusIgnoreCase(String estatus, Pageable pageable);

    Page<Cuenta> findByClienteIdAndEstatusIgnoreCase(Long clienteId, String estatus, Pageable pageable);
}
