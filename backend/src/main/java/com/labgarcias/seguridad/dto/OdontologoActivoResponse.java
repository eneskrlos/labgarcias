package com.labgarcias.seguridad.dto;

import com.labgarcias.seguridad.domain.Usuario;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * §5.1/D-19: lo que necesita el selector de odontólogo al registrar una orden, y **nada más**.
 *
 * Va aparte de `OdontologoResponse` a propósito: aquella es la ficha de la cuenta recién creada y
 * lleva correo, dirección y teléfono. Un selector no necesita ninguno de esos datos, y devolver el
 * padrón de contacto completo del laboratorio en cada apertura del formulario sería regalarlo.
 */
public record OdontologoActivoResponse(

        @Schema(example = "12")
        Long id,

        @Schema(example = "Dr. Juan Pérez")
        String nombreCompleto

) {
    public static OdontologoActivoResponse de(Usuario usuario) {
        return new OdontologoActivoResponse(usuario.getId(), usuario.getNombreCompleto());
    }
}
