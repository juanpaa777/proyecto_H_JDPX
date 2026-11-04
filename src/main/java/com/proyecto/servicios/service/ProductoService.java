package com.proyecto.servicios.service;

import com.proyecto.servicios.model.producto.ProductoDto;

import java.util.List;
import java.util.Optional;

public interface ProductoService {

    /**
     * Obtiene la lista completa de productos activos almacenados en MongoDB en formato JSON.
     *
     * @return Lista de ProductoDto
     */
    List<ProductoDto> consultarCatalogo();

    /**
     * Consulta un producto específico por su identificador de producto.
     *
     * @param idProducto Identificador del producto
     * @return Optional con el ProductoDto encontrado
     */
    Optional<ProductoDto> consultarPorIdProducto(Integer idProducto);

    /**
     * Consulta los productos asociados a un identificador de servicio.
     *
     * @param idServicio Identificador del servicio
     * @return Lista de ProductoDto
     */
    List<ProductoDto> consultarPorIdServicio(Integer idServicio);

    /**
     * Exporta el catálogo actual serializado en formato XML estándar.
     *
     * @return Cadena XML con los productos
     */
    String exportarCatalogoXml();
}
