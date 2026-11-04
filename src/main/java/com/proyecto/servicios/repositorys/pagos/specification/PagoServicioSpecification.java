package com.proyecto.servicios.repositorys.pagos.specification;

import com.proyecto.servicios.dto.pagos.ConsultaPagoServicioRequest;
import com.proyecto.servicios.entity.pagos.PagoServicio;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PagoServicioSpecification {

    private PagoServicioSpecification() {
    }

    public static Specification<PagoServicio> conFiltros(ConsultaPagoServicioRequest filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Optional.ofNullable(filtro).ifPresent(f -> {
                Optional.ofNullable(f.getId())
                        .ifPresent(v -> predicates.add(cb.equal(root.get("id"), v)));

                Optional.ofNullable(f.getClienteId())
                        .ifPresent(v -> predicates.add(cb.equal(root.get("cliente").get("id"), v)));

                Optional.ofNullable(f.getNumeroCuenta())
                        .filter(java.util.function.Predicate.not(String::isBlank))
                        .map(String::trim)
                        .ifPresent(v -> predicates.add(cb.equal(root.get("numeroCuenta"), v)));

                Optional.ofNullable(f.getUpc())
                        .filter(java.util.function.Predicate.not(String::isBlank))
                        .map(String::trim)
                        .ifPresent(v -> predicates.add(cb.equal(cb.upper(root.get("upc")), v.toUpperCase())));

                Optional.ofNullable(f.getIdServicio())
                        .ifPresent(v -> predicates.add(cb.equal(root.get("idServicio"), v)));

                Optional.ofNullable(f.getIdProducto())
                        .ifPresent(v -> predicates.add(cb.equal(root.get("idProducto"), v)));

                Optional.ofNullable(f.getEstatus())
                        .filter(java.util.function.Predicate.not(String::isBlank))
                        .map(String::trim)
                        .ifPresent(v -> predicates.add(cb.equal(cb.upper(root.get("estatus")), v.toUpperCase())));

                Optional.ofNullable(f.getFechaInicio())
                        .ifPresent(inicio -> predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), inicio)));

                Optional.ofNullable(f.getFechaFin())
                        .ifPresent(fin -> predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), fin)));
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

