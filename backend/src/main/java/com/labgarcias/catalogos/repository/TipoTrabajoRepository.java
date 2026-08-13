package com.labgarcias.catalogos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labgarcias.catalogos.domain.TipoTrabajo;

public interface TipoTrabajoRepository extends JpaRepository<TipoTrabajo, Integer> {

    /** CU-16: el odontólogo solo ve tipos activos (GET /activos, sin paginar). */
    List<TipoTrabajo> findAllByActivoTrueOrderByNombreAsc();

    /** CU-16: nombre único (uq_tipo_trabajo_nombre). */
    Optional<TipoTrabajo> findByNombre(String nombre);

    /**
     * CU-16: listado paginado de administración con filtros opcionales.
     * "translate" pasa el nombre y el término de búsqueda a minúsculas sin
     * vocales acentuadas ni ñ, para una coincidencia parcial insensible a
     * mayúsculas y acentos sin necesitar la extensión unaccent (esquema fijo).
     */
    @Query("SELECT t FROM TipoTrabajo t "
            + "WHERE (:activo IS NULL OR t.activo = :activo) "
            + "AND (:busqueda IS NULL OR "
            + "     LOWER(function('translate', t.nombre, 'ÁÉÍÓÚáéíóúÑñ', 'AEIOUaeiouNn')) "
            + "     LIKE LOWER(function('translate', CONCAT('%', CAST(:busqueda AS string), '%'), 'ÁÉÍÓÚáéíóúÑñ', 'AEIOUaeiouNn')))")
    Page<TipoTrabajo> buscar(@Param("activo") Boolean activo, @Param("busqueda") String busqueda, Pageable pageable);
}
