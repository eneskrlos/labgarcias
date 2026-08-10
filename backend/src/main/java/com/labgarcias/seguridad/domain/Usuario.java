package com.labgarcias.seguridad.domain;

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

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "correo", nullable = false, unique = true, length = 255)
    private String correo;

    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 60)
    private String nombreUsuario;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "proveedor_auth", nullable = false, length = 10)
    private ProveedorAuth proveedorAuth;

    @Column(name = "google_subject_id", unique = true, length = 255)
    private String googleSubjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuenta", nullable = false, length = 25)
    private EstadoCuenta estadoCuenta;

    @Column(name = "correo_verificado", nullable = false)
    private boolean correoVerificado;

    @Column(name = "fecha_creacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;

    public Usuario() {
    }

    public Long getId() {
        return id;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public ProveedorAuth getProveedorAuth() {
        return proveedorAuth;
    }

    public void setProveedorAuth(ProveedorAuth proveedorAuth) {
        this.proveedorAuth = proveedorAuth;
    }

    public String getGoogleSubjectId() {
        return googleSubjectId;
    }

    public void setGoogleSubjectId(String googleSubjectId) {
        this.googleSubjectId = googleSubjectId;
    }

    public EstadoCuenta getEstadoCuenta() {
        return estadoCuenta;
    }

    public void setEstadoCuenta(EstadoCuenta estadoCuenta) {
        this.estadoCuenta = estadoCuenta;
    }

    public boolean isCorreoVerificado() {
        return correoVerificado;
    }

    public void setCorreoVerificado(boolean correoVerificado) {
        this.correoVerificado = correoVerificado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
