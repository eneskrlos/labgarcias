package com.labgarcias.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** CU-01: respuesta de un login exitoso. */
public record LoginResponse(

        @Schema(description = "JWT a usar en el header Authorization: Bearer <token>")
        String token,

        @Schema(description = "§3.1.b: si es true, el token solo habilita POST /api/v1/auth/cambiar-password "
                + "y el frontend tiene que llevar al usuario a /cambiar-password.", example = "false")
        boolean debeCambiarPassword,

        UsuarioResumenResponse usuario

) {
}
