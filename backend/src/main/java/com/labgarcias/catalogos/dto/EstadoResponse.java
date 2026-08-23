package com.labgarcias.catalogos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EstadoResponse(

        @Schema(example = "3")
        Short id,

        @Schema(example = "EN_PRODUCCION")
        String codigo,

        @Schema(example = "En producción")
        String nombre,

        String descripcion,

        @Schema(description = "RN-04: posición en el flujo lineal; null si es ajeno al flujo (ej.: CANCELADO)", example = "3")
        Short ordenSecuencia,

        boolean esTerminal,

        boolean esProductivo,

        boolean activo

) {
}
