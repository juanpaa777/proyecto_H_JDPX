package com.proyecto.servicios.repositorys.onboarding.specification;

import com.proyecto.servicios.dto.onboarding.ConsultaUsuarioRequest;
import com.proyecto.servicios.entity.onboarding.Usuario;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class UsuarioSpecification {

    private UsuarioSpecification() {
    }

    public static Specification<Usuario> conFiltros(ConsultaUsuarioRequest filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Optional.ofNullable(filtro).ifPresent(f -> {
                Optional.ofNullable(f.getId())
                        .ifPresent(v -> predicates.add(cb.equal(root.get("id"), v)));

                Optional.ofNullable(f.getClienteId())
                        .ifPresent(v -> predicates.add(cb.equal(root.get("cliente").get("id"), v)));

                Optional.ofNullable(f.getCorreo())
                        .filter(java.util.function.Predicate.not(String::isBlank))
                        .map(String::trim)
                        .ifPresent(v -> predicates.add(cb.like(cb.lower(root.get("correo")), "%" + v.toLowerCase() + "%")));

                Optional.ofNullable(f.getActivo())
                        .ifPresent(v -> predicates.add(cb.equal(root.get("activo"), v)));

                Optional.ofNullable(f.getFechaInicio())
                        .ifPresent(inicio -> predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), inicio)));

                Optional.ofNullable(f.getFechaFin())
                        .ifPresent(fin -> predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), fin)));
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
