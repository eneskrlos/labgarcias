package com.labgarcias.catalogos.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "estado")
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "orden_secuencia")
    private Short ordenSecuencia;

    @Column(name = "es_terminal", nullable = false)
    private boolean esTerminal;

    @Column(name = "es_productivo", nullable = false)
    private boolean esProductivo;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "fecha_creacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;

    public Estado() {
    }

    public Short getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Short getOrdenSecuencia() {
        return ordenSecuencia;
    }

    public void setOrdenSecuencia(Short ordenSecuencia) {
        this.ordenSecuencia = ordenSecuencia;
    }

    public boolean isEsTerminal() {
        return esTerminal;
    }

    public void setEsTerminal(boolean esTerminal) {
        this.esTerminal = esTerminal;
    }

    public boolean isEsProductivo() {
        return esProductivo;
    }

    public void setEsProductivo(boolean esProductivo) {
        this.esProductivo = esProductivo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
