package com.labgarcias.ordenes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CU-02: los indicadores del panel del odontólogo, siempre sobre sus propias órdenes (RN-01).
 *
 * Son **tres, no cuatro**: el contador de "mensajes nuevos" que enumera CU-02 no se implementa
 * porque D-11 pospuso la mensajería entera.
 *
 * `enCurso` y `listasParaRetirar` **no se solapan**: un trabajo listo se cuenta una sola vez, en
 * el segundo. Así "3 en curso, 1 listo" se lee como cuatro trabajos abiertos y no como tres.
 */
public record ContadoresOdontologoResponse(

        @Schema(description = "Órdenes abiertas que todavía no están para retirar", example = "3")
        long enCurso,

        @Schema(description = "Órdenes en la etapa LISTO", example = "1")
        long listasParaRetirar,

        @Schema(description = "Órdenes cuyo pasaje a ENTREGADO cayó en la semana en curso "
                + "(lunes a domingo, en la zona horaria del laboratorio)", example = "2")
        long entregadasEstaSemana

) {
}
