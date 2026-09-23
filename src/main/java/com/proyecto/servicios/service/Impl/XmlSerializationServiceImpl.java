package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.XmlSerializationService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class XmlSerializationServiceImpl implements XmlSerializationService {

    private final ConcurrentMap<Class<?>, JAXBContext> jaxbContextCache = new ConcurrentHashMap<>();

    private JAXBContext getJAXBContext(Class<?> clazz) throws JAXBException {
        return jaxbContextCache.computeIfAbsent(clazz, c -> {
            try {
                return JAXBContext.newInstance(c);
            } catch (JAXBException e) {
                throw new RuntimeException("Error al instanciar JAXBContext para la clase: " + c.getName(), e);
            }
        });
    }

    @Override
    public <T> String serializeToXml(T object) {
        if (object == null) {
            log.warn("El objeto a serializar a XML es nulo");
            return null;
        }

        try {
            JAXBContext context = getJAXBContext(object.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());

            StringWriter writer = new StringWriter();
            marshaller.marshal(object, writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("Error al serializar objeto a XML [tipo={}]: {}", object.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("Fallo en la serialización a XML", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserializeFromXml(String xmlContent, Class<T> targetClass) {
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            log.warn("El contenido XML proporcionado para deserializar está vacío o es nulo");
            return null;
        }

        try {
            JAXBContext context = getJAXBContext(targetClass);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader reader = new StringReader(xmlContent.trim());
            return (T) unmarshaller.unmarshal(reader);
        } catch (Exception e) {
            log.error("Error al deserializar XML a la clase [{}]: {}", targetClass.getName(), e.getMessage(), e);
            throw new RuntimeException("Fallo en la deserialización desde XML", e);
        }
    }
}
