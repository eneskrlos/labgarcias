package com.labgarcias.licencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** CU-23: estado actual de la licencia de esta instalación. */
public record LicenciaVigenteResponse(

        @Schema(description = "Si hay un período de licencia vigente hoy")
        boolean vigente,

        @Schema(description = "Detalle del período vigente, o null si no hay ninguno")
        LicenciaResponse licencia

) {
}
