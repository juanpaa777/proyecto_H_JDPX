package com.proyecto.servicios.util;

import com.proyecto.servicios.model.gestopago.xml.MensajeXml;
import com.proyecto.servicios.model.gestopago.xml.ProductListXmlResponse;
import com.proyecto.servicios.model.gestopago.xml.ProductoXml;
import com.proyecto.servicios.service.Impl.XmlSerializationServiceImpl;
import com.proyecto.servicios.service.XmlSerializationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlSerializationServiceTest {

    private XmlSerializationService xmlSerializationService;

    @BeforeEach
    void setUp() {
        xmlSerializationService = new XmlSerializationServiceImpl();
    }

    @Test
    @DisplayName("Debe deserializar correctamente un XML válido de GestoPago a objetos Java")
    void testDeserializeValidXml() {
        String xmlSample = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<RESPONSE>\n" +
                "    <MENSAJE>\n" +
                "        <CODIGO>01</CODIGO>\n" +
                "        <TEXTO>Operacion realizada con exito</TEXTO>\n" +
                "    </MENSAJE>\n" +
                "    <PRODUCTOS>\n" +
                "        <producto servicio='AGUAKAN (Cancun)' producto='Agua Cancun' idServicio='56' idProducto='185' " +
                "idCatTipoServicio='15' tipoFront='2' hasDigitoVerificador='false' precio='10.0' showAyuda='false' tipoReferencia='c'>\n" +
                "            <legend><![CDATA[Para cualquier duda o aclaracion comunicarse al 073]]></legend>\n" +
                "        </producto>\n" +
                "        <producto servicio='Amazon' producto='Amazon $100' idServicio='71' idProducto='200' " +
                "idCatTipoServicio='10' tipoFront='1' hasDigitoVerificador='false' precio='100.0' showAyuda='false' tipoReferencia='a'>\n" +
                "            <legend><![CDATA[Instrucciones de canje]]></legend>\n" +
                "        </producto>\n" +
                "    </PRODUCTOS>\n" +
                "</RESPONSE>";

        ProductListXmlResponse response = xmlSerializationService.deserializeFromXml(xmlSample, ProductListXmlResponse.class);

        assertNotNull(response);
        assertNotNull(response.getMensaje());
        assertEquals("01", response.getMensaje().getCodigo());
        assertEquals("Operacion realizada con exito", response.getMensaje().getTexto());

        List<ProductoXml> productos = response.getProductos();
        assertNotNull(productos);
        assertEquals(2, productos.size());

        ProductoXml primerProducto = productos.get(0);
        assertEquals(185, primerProducto.getIdProducto());
        assertEquals(56, primerProducto.getIdServicio());
        assertEquals("AGUAKAN (Cancun)", primerProducto.getServicio());
        assertEquals("Agua Cancun", primerProducto.getProducto());
        assertEquals("10.0", primerProducto.getPrecio());
        assertEquals(2, primerProducto.getTipoFront());
        assertEquals("c", primerProducto.getTipoReferencia());
        assertEquals("Para cualquier duda o aclaracion comunicarse al 073", primerProducto.getLegend());
    }

    @Test
    @DisplayName("Debe serializar un objeto Java a un XML con formato UTF-8")
    void testSerializeToXml() {
        ProductListXmlResponse response = ProductListXmlResponse.builder()
                .mensaje(MensajeXml.builder().codigo("01").texto("Operacion exitosa").build())
                .productos(List.of(
                        ProductoXml.builder()
                                .idProducto(999)
                                .idServicio(88)
                                .servicio("Telcel")
                                .producto("Recarga 50")
                                .precio("50.0")
                                .tipoFront(1)
                                .tipoReferencia("a")
                                .legend("Instrucciones")
                                .build()
                ))
                .build();

        String xmlOutput = xmlSerializationService.serializeToXml(response);

        assertNotNull(xmlOutput);
        assertTrue(xmlOutput.contains("<RESPONSE>"));
        assertTrue(xmlOutput.contains("<CODIGO>01</CODIGO>"));
        assertTrue(xmlOutput.contains("idProducto=\"999\"") || xmlOutput.contains("idProducto='999'"));
        assertTrue(xmlOutput.contains("servicio=\"Telcel\"") || xmlOutput.contains("servicio='Telcel'"));
        assertTrue(xmlOutput.contains("<legend>Instrucciones</legend>"));
    }

    @Test
    @DisplayName("Debe manejar contenido nulo o vacío retornando null sin lanzar excepción no controlada")
    void testNullOrEmptyInput() {
        assertNull(xmlSerializationService.serializeToXml(null));
        assertNull(xmlSerializationService.deserializeFromXml(null, ProductListXmlResponse.class));
        assertNull(xmlSerializationService.deserializeFromXml("   ", ProductListXmlResponse.class));
    }

    @Test
    @DisplayName("Debe lanzar RuntimeException controlada cuando el XML está corrupto o malformado")
    void testMalformedXmlThrowsException() {
        String malformedXml = "<RESPONSE><MENSAJE><CODIGO>01</MENSAJE>"; // Etiqueta no cerrada

        assertThrows(RuntimeException.class, () -> {
            xmlSerializationService.deserializeFromXml(malformedXml, ProductListXmlResponse.class);
        });
    }
}
