package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_activas", indexes = {
        @Index(name = "idx_sesiones_usuario_activa", columnList = "usuario_id, activa"),
        @Index(name = "idx_sesiones_token_jti", columnList = "token_jti")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionActiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "token_jti", nullable = false, unique = true, length = 64)
    private String tokenJti;

    @Column(name = "plataforma", nullable = false, length = 30)
    private String plataforma;

    @Column(name = "dispositivo_info", nullable = false, length = 150)
    private String dispositivoInfo;

    @Builder.Default
    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    @Column(name = "motivo_cierre", length = 100)
    private String motivoCierre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_ultimo_acceso", nullable = false)
    private LocalDateTime fechaUltimoAcceso;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.fechaInicio == null) {
            this.fechaInicio = now;
        }
        if (this.fechaUltimoAcceso == null) {
            this.fechaUltimoAcceso = now;
        }
        if (this.activa == null) {
            this.activa = true;
        }
    }
}
