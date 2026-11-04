package com.proyecto.servicios.repositorys.pagos;

import com.proyecto.servicios.entity.pagos.PagoServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoServicioRepository extends JpaRepository<PagoServicio, Long>, JpaSpecificationExecutor<PagoServicio> {

    Optional<PagoServicio> findByUpc(String upc);

    boolean existsByUpc(String upc);

    @Query("SELECT COUNT(p) > 0 FROM PagoServicio p WHERE p.idServicio = :idServicio " +
           "AND p.telefono = :telefono " +
           "AND p.estatus IN :estatusValidos " +
           "AND p.fechaCreacion >= :fechaLimite")
    boolean existeRecargaReciente(@Param("idServicio") Integer idServicio,
                                  @Param("telefono") String telefono,
                                  @Param("estatusValidos") List<String> estatusValidos,
                                  @Param("fechaLimite") LocalDateTime fechaLimite);

    @Query("SELECT COUNT(p) > 0 FROM PagoServicio p WHERE p.idServicio = :idServicio " +
           "AND p.referencia = :referencia " +
           "AND p.montoPago = :montoPago " +
           "AND p.estatus IN :estatusValidos " +
           "AND p.fechaCreacion >= :fechaLimite")
    boolean existePagoServicioReciente(@Param("idServicio") Integer idServicio,
                                       @Param("referencia") String referencia,
                                       @Param("montoPago") BigDecimal montoPago,
                                       @Param("estatusValidos") List<String> estatusValidos,
                                       @Param("fechaLimite") LocalDateTime fechaLimite);
}

