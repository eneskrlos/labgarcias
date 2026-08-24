package com.labgarcias.ordenes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * §5.7/CU-10: cuántas órdenes hay en cada etapa, leído de `v_ordenes_por_estado`.
 *
 * Trae el **nombre además del código** por el mismo motivo que `siguienteEstado` (§5.4): el nombre
 * lo edita CU-22, así que derivarlo del código en el cliente rompería la pantalla al renombrar una
 * etapa. Las etapas sin ninguna orden vienen en cero, no faltan: es un `LEFT JOIN` en la vista.
 */
public record DistribucionEstadoResponse(

        @Schema(example = "EN_PRODUCCION")
        String estadoCodigo,

        @Schema(example = "En producción")
        String estadoNombre,

        @Schema(example = "5")
        long cantidad

) {
}
