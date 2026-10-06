package com.proyecto.servicios.model.gestopago.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductoXml {

    @XmlAttribute(name = "servicio")
    private String servicio;

    @XmlAttribute(name = "producto")
    private String producto;

    @XmlAttribute(name = "idServicio")
    private Integer idServicio;

    @XmlAttribute(name = "idProducto")
    private Integer idProducto;

    @XmlAttribute(name = "idCatTipoServicio")
    private Integer idCatTipoServicio;

    @XmlAttribute(name = "tipoFront")
    private Integer tipoFront;

    @XmlAttribute(name = "hasDigitoVerificador")
    private Boolean hasDigitoVerificador;

    @XmlAttribute(name = "precio")
    private String precio;

    @XmlAttribute(name = "showAyuda")
    private Boolean showAyuda;

    @XmlAttribute(name = "tipoReferencia")
    private String tipoReferencia;

    @XmlElement(name = "legend")
    private String legend;
}
