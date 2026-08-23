package com.labgarcias.catalogos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** CU-16 A1: activa o desactiva un tipo de trabajo sin borrarlo. */
public record CambiarEstadoTipoTrabajoRequest(

        @Schema(example = "false")
        @NotNull(message = "El campo activo es obligatorio.")
        Boolean activo

) {
}
