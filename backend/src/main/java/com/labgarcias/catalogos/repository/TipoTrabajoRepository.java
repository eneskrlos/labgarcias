package com.labgarcias.catalogos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.catalogos.domain.TipoTrabajo;

public interface TipoTrabajoRepository extends JpaRepository<TipoTrabajo, Integer> {

    /** CU-16: el odontólogo solo ve tipos activos. */
    List<TipoTrabajo> findAllByActivoTrueOrderByNombreAsc();

    List<TipoTrabajo> findAllByOrderByNombreAsc();

    /** CU-16: nombre único (uq_tipo_trabajo_nombre). */
    Optional<TipoTrabajo> findByNombre(String nombre);
}
