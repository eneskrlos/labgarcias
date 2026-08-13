package com.labgarcias.ordenes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.ordenes.domain.OrdenArchivo;

public interface OrdenArchivoRepository extends JpaRepository<OrdenArchivo, Long> {

    /** §5.2: adjuntos de una orden (idx_archivo_orden). */
    List<OrdenArchivo> findByOrdenIdOrderByFechaCargaAsc(Long ordenId);
}
