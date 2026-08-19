package com.labgarcias.ordenes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CU-04/§5.4: detalle y seguimiento de una orden. Son los datos del listado más la
 * descripción, el desglose de precios, los adjuntos y la línea de tiempo.
 */
public record OrdenDetalleResponse(

        @Schema(example = "1")
        Long id,

        @Schema(example = "LG-0001")
        String codigo,

        @Schema(description = "RN-22: iniciales + código, nunca el nombre completo", example = "M.P. - Caso #1000")
        String pacienteIdentificacion,

        // RN-22: solo el laboratorio lo recibe, porque lo necesita para operar. En la respuesta
        // al odontólogo viaja en null y NON_NULL hace que la clave ni siquiera aparezca en el
        // JSON: §5.4 criterio 3 pide que la respuesta no lo contenga, no que lo traiga vacío.
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "D-04/RN-22: presente solo para ADMIN y SUPERADMIN", example = "Martín Pérez")
        String pacienteNombre,

        @Schema(example = "DISYUNTOR CON TORNILLO ESTANDAR")
        String tipoTrabajo,

        @Schema(example = "Normal")
        String tipoOrden,

        @Schema(example = "En producción")
        String estado,

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
        BigDecimal precioTotal,

        @Schema(description = "Metadatos de los adjuntos (§5.2). El binario se descarga aparte.")
        List<OrdenArchivoResponse> archivos,

        @Schema(description = "CU-04: etapas alcanzadas, en orden cronológico")
        List<EtapaSeguimientoResponse> lineaTiempo

) {
}
