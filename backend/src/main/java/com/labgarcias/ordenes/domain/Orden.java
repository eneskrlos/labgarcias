package com.labgarcias.ordenes.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.labgarcias.catalogos.domain.Estado;
import com.labgarcias.catalogos.domain.TipoOrden;
import com.labgarcias.catalogos.domain.TipoTrabajo;
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

/** CU-09: orden creada por el odontólogo. RN-17: inmutable tras su creación, solo cancelable. */
@Entity
@Table(name = "orden")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código visible del caso (LG-XXXX). Lo genera la secuencia seq_codigo_orden. */
    @Generated(event = EventType.INSERT)
    @Column(name = "codigo", nullable = false, unique = true, length = 20, insertable = false, updatable = false)
    private String codigo;

    /** RN-01: propietario de la orden; todo acceso se filtra por él. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private Usuario odontologo;

    /** D-04: uso interno. RN-03/RN-22: no se expone en respuestas al odontólogo. */
    @Column(name = "paciente_nombre", nullable = false, length = 150)
    private String pacienteNombre;

    /** RN-22: iniciales derivadas del nombre por el backend. */
    @Column(name = "paciente_iniciales", nullable = false, length = 10)
    private String pacienteIniciales;

    /** RN-22: código autoincremental generado por la secuencia seq_codigo_paciente. */
    @Generated(event = EventType.INSERT)
    @Column(name = "paciente_codigo", nullable = false, unique = true, insertable = false, updatable = false)
    private Integer pacienteCodigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_trabajo_id", nullable = false)
    private TipoTrabajo tipoTrabajo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_orden_id", nullable = false)
    private TipoOrden tipoOrden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    /** RN-18: fecha_ingreso + días hábiles del tipo de trabajo. */
    @Column(name = "fecha_estimada_entrega", nullable = false)
    private LocalDate fechaEstimadaEntrega;

    /** CU-16 A1: foto de tipo_trabajo.dias_estimados al crear la orden. */
    @Column(name = "dias_estimados_aplicados", nullable = false)
    private Short diasEstimadosAplicados;

    /** D-14/RN-21: foto de tipo_trabajo.precio al crear la orden. */
    @Column(name = "precio_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBase;

    /** RN-11/D-14: foto de tipo_orden.recargo_monto al crear la orden. */
    @Column(name = "recargo_urgencia", nullable = false, precision = 10, scale = 2)
    private BigDecimal recargoUrgencia;

    /** Columna generada por la base (precio_base + recargo_urgencia): solo lectura. */
    @Generated(event = EventType.INSERT)
    @Column(name = "precio_total", precision = 10, scale = 2, insertable = false, updatable = false)
    private BigDecimal precioTotal;

    /** P-14: la columna existe, pero no se le aplica lógica de cálculo ni cobro. Queda en null. */
    @Column(name = "cargo_cancelacion", precision = 10, scale = 2)
    private BigDecimal cargoCancelacion;

    @Column(name = "fecha_creacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;

    /** CU-20: se sella al cancelar. */
    @Column(name = "fecha_cancelacion")
    private OffsetDateTime fechaCancelacion;

    public Orden() {
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Usuario getOdontologo() {
        return odontologo;
    }

    public void setOdontologo(Usuario odontologo) {
        this.odontologo = odontologo;
    }

    public String getPacienteNombre() {
        return pacienteNombre;
    }

    public void setPacienteNombre(String pacienteNombre) {
        this.pacienteNombre = pacienteNombre;
    }

    public String getPacienteIniciales() {
        return pacienteIniciales;
    }

    public void setPacienteIniciales(String pacienteIniciales) {
        this.pacienteIniciales = pacienteIniciales;
    }

    public Integer getPacienteCodigo() {
        return pacienteCodigo;
    }

    public TipoTrabajo getTipoTrabajo() {
        return tipoTrabajo;
    }

    public void setTipoTrabajo(TipoTrabajo tipoTrabajo) {
        this.tipoTrabajo = tipoTrabajo;
    }

    public TipoOrden getTipoOrden() {
        return tipoOrden;
    }

    public void setTipoOrden(TipoOrden tipoOrden) {
        this.tipoOrden = tipoOrden;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public LocalDate getFechaEstimadaEntrega() {
        return fechaEstimadaEntrega;
    }

    public void setFechaEstimadaEntrega(LocalDate fechaEstimadaEntrega) {
        this.fechaEstimadaEntrega = fechaEstimadaEntrega;
    }

    public Short getDiasEstimadosAplicados() {
        return diasEstimadosAplicados;
    }

    public void setDiasEstimadosAplicados(Short diasEstimadosAplicados) {
        this.diasEstimadosAplicados = diasEstimadosAplicados;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(BigDecimal precioBase) {
        this.precioBase = precioBase;
    }

    public BigDecimal getRecargoUrgencia() {
        return recargoUrgencia;
    }

    public void setRecargoUrgencia(BigDecimal recargoUrgencia) {
        this.recargoUrgencia = recargoUrgencia;
    }

    public BigDecimal getPrecioTotal() {
        return precioTotal;
    }

    public BigDecimal getCargoCancelacion() {
        return cargoCancelacion;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public OffsetDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public void setFechaCancelacion(OffsetDateTime fechaCancelacion) {
        this.fechaCancelacion = fechaCancelacion;
    }
}
