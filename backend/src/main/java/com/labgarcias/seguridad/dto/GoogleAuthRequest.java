package com.labgarcias.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** RN-16: token de identidad emitido por Google (Sign-In con Google) en el cliente. */
public record GoogleAuthRequest(

        @Schema(description = "ID token de Google obtenido en el frontend", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
        @NotBlank(message = "El idToken es obligatorio.")
        String idToken

) {
}
