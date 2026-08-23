package com.labgarcias.catalogos.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/** RN-11: solo los campos que necesita el cliente para elegir el tipo de orden. */
public record TipoOrdenResponse(

        @Schema(example = "URGENTE")
        String codigo,

        @Schema(example = "Urgente")
        String nombre,

        @Schema(description = "Monto fijo adicional (D-14); 0 para NORMAL", example = "200.00")
        BigDecimal recargoMonto

) {
}
