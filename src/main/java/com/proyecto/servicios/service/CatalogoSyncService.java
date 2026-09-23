package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.mongo.CatalogoSyncLogDocument;

public interface CatalogoSyncService {

    /**
     * Tarea programada para sincronizar el catálogo de productos desde GestoPago hacia MongoDB.
     */
    void sincronizarCatalogoProgramado();

    /**
     * Ejecuta manualmente la sincronización del catálogo bajo demanda y retorna el registro de auditoría.
     *
     * @return CatalogoSyncLogDocument con el resultado de la sincronización y XML registrado.
     */
    CatalogoSyncLogDocument sincronizarCatalogoManual();
}
