package com.proyecto.servicios.entity.onboarding;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "domicilios", indexes = {
        @Index(name = "idx_domicilios_cliente_id", columnList = "cliente_id"),
        @Index(name = "idx_domicilios_codigo_postal", columnList = "codigo_postal")
})
@Getter
@Setter
@NoArgsConstructor
public class Domicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    @JsonBackReference
    private Cliente cliente;

    @Column(name = "calle", nullable = false, length = 100)
    private String calle;

    @Column(name = "numero_exterior", nullable = false, length = 20)
    private String numeroExterior;

    @Column(name = "numero_interior", length = 20)
    private String numeroInterior;

    @Column(name = "colonia", nullable = false, length = 80)
    private String colonia;

    @Column(name = "municipio", nullable = false, length = 80)
    private String municipio;

    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    @Column(name = "codigo_postal", nullable = false, length = 5, columnDefinition = "CHAR(5)")
    private String codigoPostal;

    @Column(name = "pais", nullable = false, length = 50)
    private String pais = "México";
}
