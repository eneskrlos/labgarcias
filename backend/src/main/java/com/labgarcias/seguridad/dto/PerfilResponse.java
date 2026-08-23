package com.labgarcias.seguridad.dto;

import com.labgarcias.seguridad.domain.Usuario;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * §7: los datos propios del usuario autenticado. Incluye el estado de Telegram porque §6.5 pide
 * que el perfil muestre si la cuenta está vinculada.
 *
 * **No devuelve el `telegram_chat_id`**: al usuario no le dice nada y es el destino al que llegan
 * sus notificaciones. Alcanza con si está vinculado o no.
 */
public record PerfilResponse(

        @Schema(example = "12")
        Long id,

        @Schema(example = "Dr. Juan Pérez")
        String nombreCompleto,

        @Schema(example = "juan@mail.com")
        String correo,

        @Schema(example = "jperez")
        String nombreUsuario,

        @Schema(example = "Av. 18 de Julio 1234")
        String direccion,

        @Schema(example = "+59891234567")
        String telefono,

        @Schema(description = "Código del rol; §7 no permite cambiarlo desde el perfil", example = "ODONTOLOGO")
        String rol,

        @Schema(description = "§6.5: si la cuenta se vinculó con el bot de Telegram", example = "false")
        boolean telegramVinculado

) {
    public static PerfilResponse de(Usuario usuario) {
        return new PerfilResponse(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getNombreUsuario(),
                usuario.getDireccion(),
                usuario.getTelefono(),
                usuario.getRol().getCodigo().name(),
                usuario.isTelegramVinculado());
    }
}
