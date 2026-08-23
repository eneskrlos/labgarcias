package com.labgarcias.ordenes.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CU-04/§5.4: una etapa alcanzada de la línea de tiempo, tomada de orden_historial_estado.
 */
public record EtapaSeguimientoResponse(

        @Schema(example = "En producción")
        String estado,

        @Schema(description = "Marca temporal que sella la base al registrar la transición",
                example = "2026-08-08T10:15:30-03:00")
        OffsetDateTime fechaHora,

        @Schema(description = "Quién hizo el cambio. Null cuando lo asignó el sistema, que es el caso "
                + "del registro inicial de toda orden (§5.1 paso 9).", example = "Laura García")
        String autor

) {
}
