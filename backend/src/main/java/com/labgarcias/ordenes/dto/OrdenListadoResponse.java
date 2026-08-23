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

        @Schema(example = "2026-08-06")
        LocalDate fechaIngreso,

        @Schema(description = "RN-18: fecha de ingreso + días hábiles del tipo de trabajo", example = "2026-08-17")
        LocalDate fechaEstimadaEntrega,

        @Schema(description = "Calculado por la base: precioBase + recargoUrgencia", example = "250.00")
        BigDecimal precioTotal

) {
}
