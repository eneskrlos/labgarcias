package com.labgarcias.ordenes.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labgarcias.ordenes.domain.Orden;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    /**
     * CU-03/RN-01: órdenes de un odontólogo. El id del dueño es un parámetro del método,
     * no de la petición: lo pone siempre el service con el usuario autenticado.
     * El filtro por estado es opcional y compara contra estado.codigo (RECIBIDO,
     * EN_PRODUCCION, ...), que es el identificador estable; el nombre es texto de pantalla.
     * El grafo trae las tres relaciones que el listado convierte a texto, para no disparar
     * tres consultas por fila.
     */
    @EntityGraph(attributePaths = { "tipoTrabajo", "tipoOrden", "estado" })
    @Query("SELECT o FROM Orden o "
            + "WHERE o.odontologo.id = :odontologoId "
            + "AND (:estado IS NULL OR o.estado.codigo = :estado)")
    Page<Orden> buscarDelOdontologo(@Param("odontologoId") Long odontologoId,
                                    @Param("estado") String estado,
                                    Pageable pageable);

    /** CU-04: el detalle lee tipo de trabajo, tipo de orden y estado; se traen en la misma consulta. */
    @EntityGraph(attributePaths = { "tipoTrabajo", "tipoOrden", "estado" })
    @Query("SELECT o FROM Orden o WHERE o.id = :id")
    Optional<Orden> buscarParaDetalle(@Param("id") Long id);
}
