package com.labgarcias.ordenes.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.labgarcias.ordenes.domain.Orden;

/**
 * §5.7: los agregados de los dos paneles (CU-02 y CU-10).
 *
 * Va aparte de `OrdenRepository` porque casi nada de acá devuelve órdenes: son cuentas y bloques
 * de resumen, y dos de ellos leen **vistas** de la base en vez de la tabla.
 *
 * Las consultas que sirven a los dos paneles reciben `odontologoId`: con un valor cuentan lo de
 * ese odontólogo (CU-02 con RN-01) y con `null` lo de todo el laboratorio (CU-10). **Todo se
 * cuenta en la base**: ninguna de estas cifras se calcula trayendo órdenes a memoria.
 */
public interface DashboardRepository extends Repository<Orden, Long> {

    /**
     * CU-02/CU-10, "trabajos en curso": lo que sigue abierto y todavía no está para retirar.
     *
     * La condición de abierto sale de `estado.es_terminal`, que es donde RN-04 la define, y `LISTO`
     * se excluye por su código —el identificador estable— para que "en curso" y "listas para
     * retirar" no cuenten dos veces el mismo trabajo. Ninguna lista de estados vive en el código.
     */
    @Query("SELECT COUNT(o) FROM Orden o "
            + "WHERE o.estado.esTerminal = false AND o.estado.codigo <> :codigoListo "
            + "AND (:odontologoId IS NULL OR o.odontologo.id = :odontologoId)")
    long contarEnCurso(@Param("codigoListo") String codigoListo,
                       @Param("odontologoId") Long odontologoId);

    /** CU-02/CU-10, "listas para retirar": la etapa `LISTO` del catálogo. */
    @Query("SELECT COUNT(o) FROM Orden o WHERE o.estado.codigo = :codigoListo "
            + "AND (:odontologoId IS NULL OR o.odontologo.id = :odontologoId)")
    long contarListasParaRetirar(@Param("codigoListo") String codigoListo,
                                 @Param("odontologoId") Long odontologoId);

    /**
     * CU-02/CU-10, "entregadas esta semana": se cuenta por el **pasaje a ENTREGADO** registrado en
     * el historial, porque `orden` no tiene columna de fecha de entrega real.
     *
     * El rango es semiabierto y lo arma `SemanaLaboratorio` en el huso del laboratorio.
     * `COUNT(DISTINCT)` sobre la orden: el flujo lineal no admite dos entregas, pero contar filas
     * de historial haría que un registro repetido inflara el indicador.
     */
    @Query("SELECT COUNT(DISTINCT h.orden.id) FROM OrdenHistorialEstado h "
            + "WHERE h.estado.codigo = :codigoEntregado "
            + "AND h.fechaHora >= :desde AND h.fechaHora < :hasta "
            + "AND (:odontologoId IS NULL OR h.orden.odontologo.id = :odontologoId)")
    long contarEntregadasEntre(@Param("codigoEntregado") String codigoEntregado,
                               @Param("desde") OffsetDateTime desde,
                               @Param("hasta") OffsetDateTime hasta,
                               @Param("odontologoId") Long odontologoId);

    /**
     * CU-10, "trabajos urgentes": la vista ya define qué es una urgente activa —tipo `URGENTE` y
     * estado no terminal—, así que la condición no se reescribe en JPQL.
     */
    @Query(value = "SELECT COUNT(*) FROM v_ordenes_urgentes", nativeQuery = true)
    long contarUrgentesActivas();

    /**
     * CU-02/CU-10, "trabajos recientes". El tope y el orden llegan en el `Pageable`: es un bloque
     * de resumen, no un listado paginado de §8.1.
     */
    @EntityGraph(attributePaths = { "tipoTrabajo", "tipoOrden", "estado" })
    @Query("SELECT o FROM Orden o WHERE (:odontologoId IS NULL OR o.odontologo.id = :odontologoId)")
    List<Orden> buscarRecientes(@Param("odontologoId") Long odontologoId, Pageable pageable);

    /**
     * §5.7, "próximas a entregar", ordenadas por `fecha_estimada_entrega` (el orden va en el
     * `Pageable`). Solo las abiertas: una entregada o cancelada ya no está por entregarse, y de
     * nuevo lo decide `es_terminal`.
     */
    @EntityGraph(attributePaths = { "tipoTrabajo", "tipoOrden", "estado" })
    @Query("SELECT o FROM Orden o WHERE o.estado.esTerminal = false")
    List<Orden> buscarProximasAEntregar(Pageable pageable);

    /**
     * §5.7: distribución por estado, desde `v_ordenes_por_estado`. La vista ya trae los estados sin
     * órdenes en cero y ordenados por `orden_secuencia`. Los alias van entrecomillados porque
     * PostgreSQL pasaría a minúsculas los que no lo estén y la proyección dejaría de mapear.
     */
    @Query(value = "SELECT estado_codigo AS \"estadoCodigo\", "
            + "estado_nombre AS \"estadoNombre\", "
            + "cantidad AS \"cantidad\" "
            + "FROM v_ordenes_por_estado", nativeQuery = true)
    List<DistribucionEstadoProyeccion> distribucionPorEstado();

    /**
     * §5.7: las urgentes activas, desde `v_ordenes_urgentes`, las más próximas a entregar primero.
     *
     * **`paciente_nombre` no se selecciona** aunque la vista lo tenga: es un listado y RN-22 lo
     * prohíbe (`Agente.md` §8.2).
     */
    @Query(value = "SELECT id AS \"id\", codigo AS \"codigo\", "
            + "odontologo AS \"odontologo\", estado AS \"estado\", "
            + "fecha_estimada_entrega AS \"fechaEstimadaEntrega\" "
            + "FROM v_ordenes_urgentes "
            + "ORDER BY fecha_estimada_entrega "
            + "LIMIT :tope", nativeQuery = true)
    List<OrdenUrgenteProyeccion> buscarUrgentes(@Param("tope") int tope);
}
