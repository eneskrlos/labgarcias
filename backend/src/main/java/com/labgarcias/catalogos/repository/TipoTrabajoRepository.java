package com.labgarcias.catalogos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.catalogos.domain.TipoTrabajo;

public interface TipoTrabajoRepository extends JpaRepository<TipoTrabajo, Integer> {

    /** CU-16: el odontólogo solo ve tipos activos. */
    List<TipoTrabajo> findAllByActivoTrueOrderByNombreAsc();
}
