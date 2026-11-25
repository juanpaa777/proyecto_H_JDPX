package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "codigos_2fa", indexes = {
        @Index(name = "idx_codigos2fa_desafio", columnList = "desafio_id"),
        @Index(name = "idx_codigos2fa_usuario", columnList = "usuario_id, fecha_creacion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Codigo2fa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "desafio_id", nullable = false, unique = true, length = 64)
    private String desafioId;

    @Column(name = "codigo_hash", nullable = false, length = 255)
    private String codigoHash;

    @Column(name = "plataforma", nullable = false, length = 30)
    private String plataforma;

    @Column(name = "dispositivo_info", nullable = false, length = 150)
    private String dispositivoInfo;

    @Builder.Default
    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    @Builder.Default
    @Column(name = "usado", nullable = false)
    private Boolean usado = false;

    @Builder.Default
    @Column(name = "forzar_cierre_previa", nullable = false)
    private Boolean forzarCierrePrevia = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.intentosFallidos == null) {
            this.intentosFallidos = 0;
        }
        if (this.usado == null) {
            this.usado = false;
        }
        if (this.forzarCierrePrevia == null) {
            this.forzarCierrePrevia = false;
        }
    }
}
