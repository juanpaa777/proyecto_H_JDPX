package com.proyecto.servicios.repositorys.mongo;

import com.proyecto.servicios.entity.mongo.CatalogoSyncLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CatalogoSyncLogRepository extends MongoRepository<CatalogoSyncLogDocument, String> {

    List<CatalogoSyncLogDocument> findTop10ByOrderByFechaEjecucionDesc();

    List<CatalogoSyncLogDocument> findByFechaEjecucionBetween(LocalDateTime inicio, LocalDateTime fin);
}
