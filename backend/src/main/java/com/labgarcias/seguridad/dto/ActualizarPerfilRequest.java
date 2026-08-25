package com.labgarcias.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * §7: lo único que el usuario puede cambiar de su propio perfil.
 *
 * **No están `rol` ni `correo` a propósito**, y no por omisión: §7 lo dice explícitamente. El rol
 * decide la autorización de cada endpoint (RN-14) y el correo identifica la cuenta en el login y
 * es el destino de las notificaciones — dejarlos acá sería permitir que cualquiera se cambie los
 * permisos o se apropie de otra identidad desde su propia pantalla. Tampoco están `nombreUsuario`,
 * `telefono` ni el estado de la cuenta: §7 nombra dos campos y son estos dos.
 */
public record ActualizarPerfilRequest(

        @Schema(example = "Dr. Juan Pérez")
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres.")
        String nombreCompleto,

        @Schema(example = "Av. 18 de Julio 1234")
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres.")
        String direccion

) {
}
