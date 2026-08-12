package com.labgarcias.catalogos.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

public record TipoTrabajoResponse(

        @Schema(example = "16")
        Integer id,

        @Schema(example = "DISYUNTOR CON TORNILLO ESTANDAR")
        String nombre,

        @Schema(description = "RN-12: mínimo 7 días hábiles", example = "7")
        Integer diasEstimados,

        @Schema(description = "RN-21: mínimo 250", example = "250.00")
        BigDecimal precio,

        boolean activo

) {
}
