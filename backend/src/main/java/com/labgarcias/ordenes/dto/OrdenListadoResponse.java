package com.labgarcias.ordenes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CU-03/§5.3: ítem del listado "mis órdenes". RN-03/RN-22: identifica al paciente por
 * iniciales y código, nunca por su nombre. Es más chico que OrdenDetalleResponse a
 * propósito: el listado no necesita descripción, precios desglosados ni adjuntos.
 */
public record OrdenListadoResponse(

        @Schema(example = "1")
        Long id,

        @Schema(example = "LG-0001")
        String codigo,

        @Schema(description = "RN-22: iniciales + código, nunca el nombre completo", example = "M.P. - Caso #1000")
        String pacienteIdentificacion,

        @Schema(example = "DISYUNTOR CON TORNILLO ESTANDAR")
        String tipoTrabajo,

        @Schema(example = "Normal")
        String tipoOrden,

        @Schema(example = "En producción")
        String estado,

        /**
         * §5.3: el código de la etapa, **además** del nombre y no en su lugar.
         *
         * `estado` es el nombre visible y **CU-22 lo deja editar**. Cualquier decisión de
         * presentación que la pantalla tome por etapa —el color de su etiqueta— tiene que
         * apoyarse en algo estable, y lo estable es el código. Mismo criterio que ya aplican
         * `siguienteEstado` (§5.4) y `DistribucionEstadoResponse` (§5.7).
         */
        @Schema(description = "Código estable de la etapa. El nombre (`estado`) es editable por CU-22.",
                example = "EN_PRODUCCION")
        String estadoCodigo,

        @Schema(example = "2026-08-06")
        LocalDate fechaIngreso,

        @Schema(description = "RN-18: fecha de ingreso + días hábiles del tipo de trabajo", example = "2026-08-17")
        LocalDate fechaEstimadaEntrega,

        @Schema(description = "Calculado por la base: precioBase + recargoUrgencia", example = "250.00")
        BigDecimal precioTotal

) {
}
