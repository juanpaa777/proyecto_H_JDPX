package com.proyecto.servicios.repositorys.onboarding.specification;

import com.proyecto.servicios.dto.onboarding.ConsultaClienteRequest;
import com.proyecto.servicios.entity.onboarding.Cliente;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ClienteSpecification {

    private ClienteSpecification() {
    }

    public static Specification<Cliente> conFiltros(ConsultaClienteRequest filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Optional.ofNullable(filtro).ifPresent(f -> {
                Optional.ofNullable(f.getId())
                        .ifPresent(v -> predicates.add(cb.equal(root.get("id"), v)));

                Optional.ofNullable(f.getNombre())
                        .filter(java.util.function.Predicate.not(String::isBlank))
                        .map(String::trim)
                        .ifPresent(v -> predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + v.toLowerCase() + "%")));

                Optional.ofNullable(f.getApellidoPaterno())
                        .filter(java.util.function.Predicate.not(String::isBlank))
                        .map(String::trim)
                        .ifPresent(v -> predicates.add(cb.like(cb.lower(root.get("apellidoPaterno")), "%" + v.toLowerCase() + "%")));

                Optional.ofNullable(f.getApellidoMaterno())
                        .filter(java.util.function.Predicate.not(String::isBlank))
                        .map(String::trim)
                        .ifPresent(v -> predicates.add(cb.like(cb.lower(root.get("apellidoMaterno")), "%" + v.toLowerCase() + "%")));

                Optional.ofNullable(f.getCurp())
                        .filter(java.util.function.Predicate.not(String::isBlank))
                        .map(String::trim)
                        .ifPresent(v -> predicates.add(cb.equal(cb.upper(root.get("curp")), v.toUpperCase())));

                Optional.ofNullable(f.getRfc())
                        .filter(java.util.function.Predicate.not(String::isBlank))
                        .map(String::trim)
                        .ifPresent(v -> predicates.add(cb.equal(cb.upper(root.get("rfc")), v.toUpperCase())));

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
