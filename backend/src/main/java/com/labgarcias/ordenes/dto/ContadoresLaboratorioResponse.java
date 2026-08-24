package com.labgarcias.ordenes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * §5.7/CU-10: los cuatro indicadores del dashboard del laboratorio, sobre las órdenes de todos.
 *
 * Los tres primeros son los mismos de CU-02 con el filtro por odontólogo quitado.
 * `urgentesActivas` **sí se solapa** con `enCurso` a propósito: son las mismas órdenes miradas por
 * otro corte —tipo `URGENTE`, estado no terminal— y así lo define `v_ordenes_urgentes`.
 */
public record ContadoresLaboratorioResponse(

        @Schema(description = "Órdenes abiertas que todavía no están para retirar", example = "12")
        long enCurso,

        @Schema(description = "Órdenes en la etapa LISTO", example = "4")
        long listasParaRetirar,

        @Schema(description = "Órdenes cuyo pasaje a ENTREGADO cayó en la semana en curso "
                + "(lunes a domingo, en la zona horaria del laboratorio)", example = "7")
        long entregadasEstaSemana,

        @Schema(description = "Urgentes sin terminar, según la vista v_ordenes_urgentes", example = "2")
        long urgentesActivas

) {
}
