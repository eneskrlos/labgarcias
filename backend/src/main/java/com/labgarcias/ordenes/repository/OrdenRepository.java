package com.labgarcias.ordenes.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labgarcias.catalogos.domain.CodigoTipoOrden;
import com.labgarcias.ordenes.domain.Orden;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    /**
     * CU-03/RN-01: órdenes de un odontólogo. El id del dueño es un parámetro del método,
     * no de la petición: lo pone siempre el service con el usuario autenticado.
     * El filtro por estado es opcional y compara contra estado.codigo (RECIBIDO,
     * EN_PRODUCCION, ...), que es el identificador estable; el nombre es texto de pantalla.
     * El grafo trae las tres relaciones que el listado convierte a texto, para no disparar
     * tres consultas por fila.
     *
     * CU-12: con `historico` en true quedan solo las órdenes cerradas. Qué es "cerrada" lo dice
     * `estado.es_terminal`, que es donde RN-04 lo define; en el código no hay ninguna lista de
     * estados terminales. Los dos filtros se combinan: `historico=true&estado=CANCELADO` deja
     * las canceladas. Por defecto va en false, así que "Mis trabajos" (§5.3) no cambia.
     */
    @EntityGraph(attributePaths = { "tipoTrabajo", "tipoOrden", "estado" })
    @Query("SELECT o FROM Orden o "
            + "WHERE o.odontologo.id = :odontologoId "
            + "AND (:estado IS NULL OR o.estado.codigo = :estado) "
            + "AND (:historico = false OR o.estado.esTerminal = true)")
    Page<Orden> buscarDelOdontologo(@Param("odontologoId") Long odontologoId,
                                    @Param("estado") String estado,
                                    @Param("historico") boolean historico,
                                    Pageable pageable);

    /**
     * CU-06/§5.7: las órdenes del laboratorio, con los tres filtros opcionales de la spec.
     * A diferencia de `buscarDelOdontologo`, acá el odontólogo **sí** es un filtro que llega de
     * la petición: quien consulta es el laboratorio, que ve todas (RN-01 no aplica a su rol).
     * Los filtros comparan contra los códigos, que son los identificadores estables.
     */
    @EntityGraph(attributePaths = { "tipoTrabajo", "tipoOrden", "estado" })
    @Query("SELECT o FROM Orden o "
            + "WHERE (:estado IS NULL OR o.estado.codigo = :estado) "
            + "AND (:tipoOrden IS NULL OR o.tipoOrden.codigo = :tipoOrden) "
            + "AND (:odontologoId IS NULL OR o.odontologo.id = :odontologoId)")
    Page<Orden> buscarParaAdministracion(@Param("estado") String estado,
                                         @Param("tipoOrden") CodigoTipoOrden tipoOrden,
                                         @Param("odontologoId") Long odontologoId,
                                         Pageable pageable);

    /** CU-04: el detalle lee tipo de trabajo, tipo de orden y estado; se traen en la misma consulta. */
    @EntityGraph(attributePaths = { "tipoTrabajo", "tipoOrden", "estado" })
    @Query("SELECT o FROM Orden o WHERE o.id = :id")
    Optional<Orden> buscarParaDetalle(@Param("id") Long id);
}
