package com.labgarcias.notificaciones.domain;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.labgarcias.seguridad.domain.Usuario;

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
 * §6.1: la mitad "mensaje" del Transactional Outbox. Es también lo que alimenta la campana
 * de §6.4: existe aunque todos sus envíos fallen, que es el criterio 2 de §6.
 *
 * Dos referencias, dos formas distintas a propósito:
 * - `destinatario` es una relación porque este módulo lo necesita para operar (RN-19: sus
 *   canales; §6.3: su correo).
 * - `ordenId` es un simple Long. La notificación solo lo usa para que la campana enlace a la
 *   orden, y mapearlo como relación ataría el módulo de notificaciones al de órdenes, que hoy
 *   solo lo alcanza por eventos (Agente.md 5.4 y 5.6).
 */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @Column(name = "orden_id")
    private Long ordenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private TipoEvento tipoEvento;

    /**
     * §3.1.b: este campo se persiste, así que nunca puede contener una contraseña temporal
     * ni datos que RN-03/RN-22 protegen (el nombre del paciente). Los textos se arman en
     * TextosNotificacion, que es el único lugar donde se decide qué dice.
     */
    @Column(name = "mensaje", nullable = false)
    private String mensaje;

    @Column(name = "leida", nullable = false)
    private boolean leida;

    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_creacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_lectura")
    private OffsetDateTime fechaLectura;

    public Notificacion() {
    }

    public Long getId() {
        return id;
    }

    public Usuario getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Usuario destinatario) {
        this.destinatario = destinatario;
    }

    public Long getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(Long ordenId) {
        this.ordenId = ordenId;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEvento tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaLectura() {
        return fechaLectura;
    }

    public void setFechaLectura(OffsetDateTime fechaLectura) {
        this.fechaLectura = fechaLectura;
    }
}
