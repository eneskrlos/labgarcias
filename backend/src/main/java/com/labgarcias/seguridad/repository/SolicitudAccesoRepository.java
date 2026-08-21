package com.labgarcias.seguridad.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.seguridad.domain.EstadoSolicitud;
import com.labgarcias.seguridad.domain.SolicitudAcceso;

public interface SolicitudAccesoRepository extends JpaRepository<SolicitudAcceso, Long> {

    /** §3.1: un correo con solicitud pendiente no puede duplicarla (criterio 3). */
    boolean existsByCorreoIgnoreCaseAndEstado(String correo, EstadoSolicitud estado);

    /** §3.1.b: listado del administrador, filtrable por estado. */
    Page<SolicitudAcceso> findByEstado(EstadoSolicitud estado, Pageable pageable);
}
