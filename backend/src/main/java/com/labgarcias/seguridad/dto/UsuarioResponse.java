package com.labgarcias.seguridad.dto;

import com.labgarcias.seguridad.domain.Usuario;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CU-17/§7: una cuenta del padrón, como la ve el SUPERADMIN en el mantenimiento del sistema.
 *
 * Va aparte de `UsuarioResumenResponse` —que es la identificación mínima embebida en el login— y de
 * `OdontologoResponse` —la ficha de una cuenta recién creada—: esta lleva el **rol**, porque el
 * padrón del SUPERADMIN abarca cuentas de cualquier rol y sin él la tabla no se entiende.
 *
 * **Sin `passwordHash` ni `telegramChatId`**, como todas las respuestas de este módulo.
 */
public record UsuarioResponse(

        @Schema(example = "12")
        Long id,

        @Schema(example = "Dr. Juan Pérez")
        String nombreCompleto,

        @Schema(example = "juan@mail.com")
        String correo,

        @Schema(example = "jperez")
        String nombreUsuario,

        @Schema(description = "Código del rol", example = "ODONTOLOGO")
        String rol,

        @Schema(description = "ACTIVA o INACTIVA", example = "ACTIVA")
        String estadoCuenta

) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getNombreUsuario(),
                usuario.getRol().getCodigo().name(),
                usuario.getEstadoCuenta().name());
    }
}
