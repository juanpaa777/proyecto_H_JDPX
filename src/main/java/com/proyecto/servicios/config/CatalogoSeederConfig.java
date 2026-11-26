package com.proyecto.servicios.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@Slf4j
public class CatalogoSeederConfig {

    @Bean
    public ApplicationRunner seedCatalogoProductosSiVacio(ProductoMongoRepository productoRepository) {
        return args -> {
            try {
                if (productoRepository.count() > 0) {
                    return;
                }
                ClassPathResource resource = new ClassPathResource("productos_seed.json");
                if (!resource.exists()) {
                    return;
                }
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                try (InputStream is = resource.getInputStream()) {
                    List<ProductoDocument> productos = mapper.readValue(
                            is,
                            new TypeReference<List<ProductoDocument>>() {}
                    );
                    LocalDateTime now = LocalDateTime.now();
                    for (ProductoDocument p : productos) {
                        p.setId(null);
                        if (p.getActivo() == null) {
                            p.setActivo(true);
                        }
                        if (p.getFechaCreacion() == null) {
                            p.setFechaCreacion(now);
                        }
                        if (p.getFechaActualizacion() == null) {
                            p.setFechaActualizacion(now);
                        }
                    }
                    productoRepository.saveAll(productos);
                    log.info("Catálogo inicial cargado en MongoDB: {} productos de GestoPago.", productos.size());
                }
            } catch (Exception e) {
                log.warn("No se pudo ejecutar el seeding automático del catálogo en MongoDB: {}", e.getMessage());
            }
        };
    }
}
