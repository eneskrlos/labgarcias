package com.labgarcias.notificaciones.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * §6.1: la mitad "entrega" del Transactional Outbox — un registro por canal. Nace PENDIENTE
 * dentro de la transacción del evento y el despachador lo resuelve después, así que un SMTP
 * caído no puede borrar el hecho de que había algo para avisar.
 */
@Entity
@Table(name = "notificacion_envio")
public class NotificacionEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notificacion_id", nullable = false)
    private Notificacion notificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 10)
    private Canal canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false, length = 10)
    private EstadoEnvio estadoEnvio;

    @Column(name = "fecha_envio")
    private OffsetDateTime fechaEnvio;

    @Column(name = "detalle_error")
    private String detalleError;

    public NotificacionEnvio() {
    }

    /** §6.1 paso 5: el envío se resolvió bien. */
    public void marcarEnviado() {
        this.estadoEnvio = EstadoEnvio.ENVIADO;
        this.fechaEnvio = OffsetDateTime.now();
        this.detalleError = null;
    }

    /** §6.1 paso 5: queda el motivo para diagnóstico; la notificación sigue viva igual. */
    public void marcarFallido(String detalleError) {
        this.estadoEnvio = EstadoEnvio.FALLIDO;
        this.fechaEnvio = OffsetDateTime.now();
        this.detalleError = detalleError;
    }

    public Long getId() {
        return id;
    }

    public Notificacion getNotificacion() {
        return notificacion;
    }

    public void setNotificacion(Notificacion notificacion) {
        this.notificacion = notificacion;
    }

    public Canal getCanal() {
        return canal;
    }

    public void setCanal(Canal canal) {
        this.canal = canal;
    }

    public EstadoEnvio getEstadoEnvio() {
        return estadoEnvio;
    }

    public void setEstadoEnvio(EstadoEnvio estadoEnvio) {
        this.estadoEnvio = estadoEnvio;
    }

    public OffsetDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public String getDetalleError() {
        return detalleError;
    }
}
