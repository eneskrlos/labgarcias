package com.labgarcias.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Identificación mínima del usuario autenticado, embebida en la respuesta de login. */
public record UsuarioResumenResponse(

        @Schema(description = "Id del usuario", example = "1")
        Long id,

        @Schema(description = "Nombre completo", example = "Dr. Juan Pérez")
        String nombreCompleto,

        @Schema(description = "Código del rol", example = "ODONTOLOGO")
        String rol

) {
}
