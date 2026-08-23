package com.labgarcias.ordenes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** CU-06/§5.5: estado al que se quiere avanzar la orden. */
public record CambiarEstadoOrdenRequest(

        // Sin lista de códigos permitidos acá: el catálogo de estados vive en la tabla
        // (§4.2, cerrado en 7) y es ella la que decide si la transición es válida (RN-04).
        @Schema(description = "Código del estado destino", example = "EN_PRODUCCION")
        @NotBlank(message = "El estado destino es obligatorio.")
        String estadoCodigo

) {
}
