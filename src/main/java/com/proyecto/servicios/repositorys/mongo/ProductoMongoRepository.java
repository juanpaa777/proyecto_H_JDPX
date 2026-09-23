package com.proyecto.servicios.repositorys.mongo;

import com.proyecto.servicios.entity.mongo.ProductoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoMongoRepository extends MongoRepository<ProductoDocument, String> {

    List<ProductoDocument> findAllByActivoTrue();

    Optional<ProductoDocument> findByIdProducto(Integer idProducto);

    List<ProductoDocument> findByIdServicioAndActivoTrue(Integer idServicio);

    List<ProductoDocument> findByIdCatTipoServicioAndActivoTrue(Integer idCatTipoServicio);
}
