package com.labgarcias.catalogos.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** CU-16: alta/edición de un tipo de trabajo. RN-12/RN-21 se validan en el service (código específico). */
public record TipoTrabajoRequest(

        @Schema(example = "PLACA ACTIVA")
        @NotBlank(message = "El nombre es obligatorio.")
        String nombre,

        @Schema(description = "RN-12: mínimo 7 días hábiles", example = "7")
        @NotNull(message = "Los días estimados son obligatorios.")
        Integer diasEstimados,

        @Schema(description = "RN-21: mínimo 250", example = "250.00")
        @NotNull(message = "El precio es obligatorio.")
        BigDecimal precio

) {
}
