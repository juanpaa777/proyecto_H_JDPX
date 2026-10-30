package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cuenta c WHERE c.numeroCuenta = :numeroCuenta")
    Optional<Cuenta> findByNumeroCuentaForUpdate(@Param("numeroCuenta") String numeroCuenta);

    boolean existsByNumeroCuenta(String numeroCuenta);

    Page<Cuenta> findByClienteId(Long clienteId, Pageable pageable);

    Page<Cuenta> findByEstatusIgnoreCase(String estatus, Pageable pageable);

    Page<Cuenta> findByClienteIdAndEstatusIgnoreCase(Long clienteId, String estatus, Pageable pageable);
}
