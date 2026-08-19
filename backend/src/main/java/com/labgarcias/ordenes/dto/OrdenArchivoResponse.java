package com.labgarcias.ordenes.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/** RN-13: metadatos de un adjunto. El binario se descarga aparte (GET /archivos/{id}). */
public record OrdenArchivoResponse(

        @Schema(example = "1")
        Long id,

        @Schema(description = "Nombre con el que se subió el archivo", example = "radiografia-panoramica.jpg")
        String nombreOriginal,

        @Schema(description = "IMAGEN o DOCUMENTO", example = "IMAGEN")
        String categoria,

        @Schema(example = "image/jpeg")
        String tipoMime,

        @Schema(example = "1048576")
        Long tamanoBytes,

        OffsetDateTime fechaCarga

) {
}
