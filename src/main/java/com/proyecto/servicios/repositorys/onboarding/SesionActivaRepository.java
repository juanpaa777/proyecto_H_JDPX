package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.SesionActiva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SesionActivaRepository extends JpaRepository<SesionActiva, Long> {

    List<SesionActiva> findByUsuarioIdAndActivaTrue(Long usuarioId);

    Optional<SesionActiva> findByTokenJti(String tokenJti);
}

