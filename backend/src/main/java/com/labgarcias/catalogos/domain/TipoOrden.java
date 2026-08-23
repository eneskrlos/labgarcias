package com.labgarcias.catalogos.domain;

import java.math.BigDecimal;

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

/** RN-11: el comportamiento diferencial de NORMAL/URGENTE es dato, no código. */
@Entity
@Table(name = "tipo_orden")
public class TipoOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Enumerated(EnumType.STRING)
    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private CodigoTipoOrden codigo;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_inicial_id", nullable = false)
    private Estado estadoInicial;

    @Column(name = "notifica_admin", nullable = false)
    private boolean notificaAdmin;

    @Column(name = "recargo_monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal recargoMonto;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    public TipoOrden() {
    }

    public Short getId() {
        return id;
    }

    public CodigoTipoOrden getCodigo() {
        return codigo;
    }

    public void setCodigo(CodigoTipoOrden codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Estado getEstadoInicial() {
        return estadoInicial;
    }

    public void setEstadoInicial(Estado estadoInicial) {
        this.estadoInicial = estadoInicial;
    }

    public boolean isNotificaAdmin() {
        return notificaAdmin;
    }

    public void setNotificaAdmin(boolean notificaAdmin) {
        this.notificaAdmin = notificaAdmin;
    }

    public BigDecimal getRecargoMonto() {
        return recargoMonto;
    }

    public void setRecargoMonto(BigDecimal recargoMonto) {
        this.recargoMonto = recargoMonto;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
