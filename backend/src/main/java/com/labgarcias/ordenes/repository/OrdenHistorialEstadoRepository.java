package com.labgarcias.ordenes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.ordenes.domain.OrdenHistorialEstado;

public interface OrdenHistorialEstadoRepository extends JpaRepository<OrdenHistorialEstado, Long> {

    /**
     * CU-04: línea de tiempo de la orden, en orden cronológico (idx_historial_orden).
     * El grafo trae estado y autor: cada etapa los muestra, y sin esto serían dos
     * consultas por etapa. El autor es opcional, así que va como LEFT JOIN.
     */
    @EntityGraph(attributePaths = { "estado", "usuario" })
    List<OrdenHistorialEstado> findByOrdenIdOrderByFechaHoraAsc(Long ordenId);
}
