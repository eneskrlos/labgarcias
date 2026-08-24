package com.labgarcias.ordenes.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * §5.7/CU-10: el dashboard del laboratorio, entero, en una sola respuesta.
 *
 * Las cuatro cosas que enumera §5.7 —contadores, distribución por estado, próximas a entregar y
 * órdenes recientes— más el bloque de urgentes de `v_ordenes_urgentes`. Ninguna se recalcula en el
 * cliente (§8).
 *
 * **Sin reportes ni estadísticas** más allá de esto: CU-13 es Fase 4.
 */
public record DashboardAdminResponse(

        ContadoresLaboratorioResponse contadores,

        @Schema(description = "Una fila por etapa del catálogo, incluidas las que están en cero")
        List<DistribucionEstadoResponse> distribucionPorEstado,

        @Schema(description = "Órdenes abiertas con la entrega estimada más cercana primero")
        List<OrdenListadoResponse> proximasAEntregar,

        @Schema(description = "Últimas órdenes del laboratorio por fecha de ingreso")
        List<OrdenListadoResponse> ordenesRecientes,

        @Schema(description = "Urgentes sin terminar, las más próximas a entregar primero")
        List<OrdenUrgenteResponse> urgentes

) {
}
