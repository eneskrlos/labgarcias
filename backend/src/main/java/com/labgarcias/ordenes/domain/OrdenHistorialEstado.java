package com.labgarcias.ordenes.domain;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.labgarcias.catalogos.domain.Estado;
import com.labgarcias.seguridad.domain.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** CU-04: alimenta la línea de tiempo del seguimiento. CU-06: registra cada transición. */
@Entity
@Table(name = "orden_historial_estado")
public class OrdenHistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    /** Autor del cambio. Null cuando lo asigna el sistema (estado inicial de la orden, RN-11). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** CU-04: la línea de tiempo necesita la marca temporal que sella la base. */
    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_hora", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaHora;

    @Column(name = "observacion")
    private String observacion;

    public OrdenHistorialEstado() {
    }

    public Long getId() {
        return id;
    }

    public Orden getOrden() {
        return orden;
    }

    public void setOrden(Orden orden) {
        this.orden = orden;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public OffsetDateTime getFechaHora() {
        return fechaHora;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
