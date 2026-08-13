package com.labgarcias.ordenes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.ordenes.domain.OrdenHistorialEstado;

public interface OrdenHistorialEstadoRepository extends JpaRepository<OrdenHistorialEstado, Long> {

    /** CU-04: línea de tiempo de la orden, en orden cronológico (idx_historial_orden). */
    List<OrdenHistorialEstado> findByOrdenIdOrderByFechaHoraAsc(Long ordenId);
}
