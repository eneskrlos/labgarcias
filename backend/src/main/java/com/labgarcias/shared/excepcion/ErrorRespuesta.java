package com.labgarcias.shared.excepcion;

import io.swagger.v3.oas.annotations.media.Schema;

/** Cuerpo uniforme de error de la API (spec.md §1). */
public record ErrorRespuesta(

        @Schema(description = "Código estable del error", example = "TIPO_TRABAJO_INACTIVO")
        String codigo,

        @Schema(description = "Mensaje legible para mostrar al usuario", example = "El tipo de trabajo seleccionado no está activo.")
        String mensaje,

        @Schema(description = "Campo del request asociado al error, si corresponde", example = "tipoTrabajoId")
        String campo

) {
    public ErrorRespuesta(String codigo, String mensaje) {
        this(codigo, mensaje, null);
    }
}
