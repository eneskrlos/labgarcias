package com.labgarcias.ordenes.repository;

/**
 * §5.7: una fila de `v_ordenes_por_estado`.
 *
 * Es una proyección de lectura sobre una **vista**, no una entidad: mapearla con `@Entity` la
 * volvería escribible y `ddl-auto: validate` pasaría a vigilar una tabla que no existe.
 * Los alias de la consulta van entrecomillados para que PostgreSQL conserve el camelCase.
 */
public interface DistribucionEstadoProyeccion {

    String getEstadoCodigo();

    String getEstadoNombre();

    long getCantidad();
}
