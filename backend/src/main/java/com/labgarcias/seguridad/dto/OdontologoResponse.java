package com.labgarcias.seguridad.dto;

import com.labgarcias.seguridad.domain.Usuario;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * §3.1.b: la cuenta recién creada, tal como la ve el administrador.
 *
 * **No incluye la contraseña temporal, ni siquiera enmascarada.** §3.1.b la manda por correo al
 * odontólogo: devolverla acá la dejaría en la respuesta HTTP, en la consola del navegador y en
 * cualquier registro de tráfico.
 */
public record OdontologoResponse(

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

        @Schema(description = "Siempre ACTIVA: el alta por el administrador es la verificación (D-18)",
                example = "ACTIVA")
        String estadoCuenta,

        @Schema(description = "Siempre true al crear: §3.1.b obliga a cambiarla en el primer ingreso",
                example = "true")
        boolean debeCambiarPassword

) {
    public static OdontologoResponse de(Usuario usuario) {
        return new OdontologoResponse(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getNombreUsuario(),
                usuario.getDireccion(),
                usuario.getTelefono(),
                usuario.getEstadoCuenta().name(),
                usuario.isDebeCambiarPassword());
    }
}
