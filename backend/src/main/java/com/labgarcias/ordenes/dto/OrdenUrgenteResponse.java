package com.labgarcias.ordenes.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * §5.7/CU-10: una urgente activa del bloque del dashboard, leída de `v_ordenes_urgentes`.
 *
 * **Sin nombre de paciente y sin identificación por iniciales**: es un listado (RN-22,
 * `Agente.md` §8.2) y la vista no expone las iniciales ni el código de caso. Para ubicar el
 * trabajo alcanza con el código de la orden, que es el que enlaza al detalle.
 */
public record OrdenUrgenteResponse(

        @Schema(example = "7")
        Long id,

        @Schema(example = "LG-0007")
        String codigo,

        @Schema(description = "Dueño de la orden. El dashboard es del laboratorio, que las ve todas.",
                example = "Dr. Juan Pérez")
        String odontologo,

        @Schema(example = "En producción")
        String estado,

        /**
         * V3/bloque 4 de la etapa 2: se suma, no reemplaza — mismo criterio que `estadoCodigo` en
         * `OrdenListadoResponse` (bloque 0). Cierra el punto que había quedado abierto en
         * `docs/ESTADO.md`: la vista ya hacía el JOIN contra `estado`, solo faltaba seleccionarlo.
         */
        @Schema(description = "Código estable de la etapa. El nombre (`estado`) es editable por CU-22.",
                example = "EN_PRODUCCION")
        String estadoCodigo,

        @Schema(example = "2026-08-28")
        LocalDate fechaEstimadaEntrega

) {
}
