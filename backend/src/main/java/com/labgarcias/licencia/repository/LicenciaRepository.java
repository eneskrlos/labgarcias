package com.labgarcias.licencia.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.labgarcias.licencia.domain.EstadoLicencia;
import com.labgarcias.licencia.domain.Licencia;

public interface LicenciaRepository extends JpaRepository<Licencia, Long> {

    /** CU-23: listado histórico, más reciente primero. */
    List<Licencia> findAllByOrderByFechaRegistroDesc();

    /** RN-20: período(s) ACTIVA cuyo rango de fechas cubre hoy. */
    List<Licencia> findByEstadoAndFechaInicioLessThanEqualAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoDesc(
            EstadoLicencia estado, LocalDate fechaInicioMaxima, LocalDate fechaVencimientoMinima);

    /** RN-20: chequeo liviano usado por el filtro de bloqueo, tal como especifica spec.md §3.6. */
    @Query(value = "SELECT licencia_activa FROM v_licencia_vigente", nativeQuery = true)
    boolean existeLicenciaVigente();
}
