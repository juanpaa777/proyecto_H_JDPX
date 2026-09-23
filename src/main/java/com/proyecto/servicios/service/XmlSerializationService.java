package com.proyecto.servicios.service;

public interface XmlSerializationService {

    /**
     * Serializa un objeto Java anotado con JAXB a una cadena XML con formato.
     *
     * @param object Objeto a serializar
     * @param <T>    Tipo de objeto
     * @return Cadena con el contenido XML formateado en UTF-8
     */
    <T> String serializeToXml(T object);

    /**
     * Deserializa una cadena XML a una instancia del tipo de clase especificado.
     *
     * @param xmlContent Cadena con el contenido XML
     * @param targetClass Clase destino
     * @param <T>        Tipo de objeto
     * @return Instancia deserializada del objeto
     */
    <T> T deserializeFromXml(String xmlContent, Class<T> targetClass);
}
