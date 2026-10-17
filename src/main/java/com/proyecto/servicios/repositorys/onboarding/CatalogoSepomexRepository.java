package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.CatalogoSepomex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoSepomexRepository extends JpaRepository<CatalogoSepomex, Long> {

    List<CatalogoSepomex> findByCodigoPostal(String codigoPostal);

    @Query("SELECT DISTINCT s.asentamiento FROM CatalogoSepomex s WHERE s.codigoPostal = :codigoPostal ORDER BY s.asentamiento ASC")
    List<String> findColoniasByCodigoPostal(@Param("codigoPostal") String codigoPostal);

    @Query("SELECT DISTINCT s.estado FROM CatalogoSepomex s ORDER BY s.estado ASC")
    List<String> findDistinctEstados();

    @Query("SELECT DISTINCT s.municipio FROM CatalogoSepomex s WHERE LOWER(s.estado) = LOWER(:estado) ORDER BY s.municipio ASC")
    List<String> findDistinctMunicipiosByEstado(@Param("estado") String estado);

    boolean existsByCodigoPostal(String codigoPostal);
}
