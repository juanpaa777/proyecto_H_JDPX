package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "catalogo_sepomex", indexes = {
        @Index(name = "idx_sepomex_cp", columnList = "codigo_postal"),
        @Index(name = "idx_sepomex_estado_mun", columnList = "estado, municipio")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoSepomex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_postal", length = 5, nullable = false)
    private String codigoPostal;

    @Column(name = "asentamiento", length = 150, nullable = false)
    private String asentamiento;

    @Column(name = "tipo_asentamiento", length = 50)
    private String tipoAsentamiento;

    @Column(name = "municipio", length = 100, nullable = false)
    private String municipio;

    @Column(name = "estado", length = 100, nullable = false)
    private String estado;

    @Column(name = "ciudad", length = 100)
    private String ciudad;
}
