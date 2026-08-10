package com.labgarcias.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** CU-01: respuesta de un login exitoso. */
public record LoginResponse(

        @Schema(description = "JWT a usar en el header Authorization: Bearer <token>")
        String token,

        UsuarioResumenResponse usuario

) {
}
