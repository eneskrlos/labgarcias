package com.labgarcias.ordenes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/** RN-03/RN-22: formato público de la orden. No incluye pacienteNombre, por diseño. */
public record OrdenResponse(

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

        @Schema(example = "Recibido")
        String estado,

        /**
         * §5.1: el código de la etapa inicial, **además** del nombre y no en su lugar.
         *
         * `estado` es el nombre visible y **CU-22 lo deja editar**; el código es lo estable.
         * Mismo criterio que `siguienteEstado` (§5.4) y `DistribucionEstadoResponse` (§5.7).
         */
        @Schema(description = "Código estable de la etapa. El nombre (`estado`) es editable por CU-22.",
                example = "RECIBIDO")
        String estadoCodigo,

        @Schema(example = "Disyuntor superior, tornillo de expansión 7mm")
        String descripcion,

        @Schema(example = "2026-08-06")
        LocalDate fechaIngreso,

        @Schema(description = "RN-18: fecha de ingreso + días hábiles del tipo de trabajo", example = "2026-08-17")
        LocalDate fechaEstimadaEntrega,

        @Schema(description = "D-14: foto del precio del catálogo al crear la orden", example = "250.00")
        BigDecimal precioBase,

        @Schema(description = "RN-11: recargo tomado de tipo_orden", example = "0.00")
        BigDecimal recargoUrgencia,

        @Schema(description = "Calculado por la base: precioBase + recargoUrgencia", example = "250.00")
        BigDecimal precioTotal

) {
}
