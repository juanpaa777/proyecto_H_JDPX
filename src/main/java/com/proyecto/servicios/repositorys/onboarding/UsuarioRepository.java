package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    Optional<Usuario> findByClienteId(Long clienteId);

    @Query("SELECT u FROM Usuario u WHERE " +
           "(:clienteId IS NULL OR u.cliente.id = :clienteId) AND " +
           "(:correo IS NULL OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :correo, '%'))) AND " +
           "(:activo IS NULL OR u.activo = :activo)")
    List<Usuario> filtrarUsuarios(@Param("clienteId") Long clienteId,
                                  @Param("correo") String correo,
                                  @Param("activo") Boolean activo);
}
