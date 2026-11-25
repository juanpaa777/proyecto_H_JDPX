package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Codigo2fa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface Codigo2faRepository extends JpaRepository<Codigo2fa, Long> {

    Optional<Codigo2fa> findByDesafioId(String desafioId);

    long countByUsuarioIdAndFechaCreacionAfter(Long usuarioId, LocalDateTime desde);
}

