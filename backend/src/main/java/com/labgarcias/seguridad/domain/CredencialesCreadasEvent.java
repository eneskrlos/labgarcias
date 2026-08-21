package com.labgarcias.seguridad.domain;

/**
 * D-18/§3.1.b paso 4: se publica al crear la cuenta de un odontólogo.
 *
 * Es el único lugar por donde circula la contraseña temporal, y circula **en memoria**: el
 * listener la usa para componer el correo y ahí termina. Nunca se persiste —`notificacion.mensaje`
 * guarda un texto genérico— ni se escribe en un log.
 *
 * Por eso `toString` está sobrescrito: un `log.debug(evento)` descuidado, o el volcado de una
 * excepción que incluya el evento, imprimirían la contraseña en claro.
 */
public record CredencialesCreadasEvent(Long usuarioId,
                                       String correo,
                                       String nombreUsuario,
                                       String passwordTemporal) {

    @Override
    public String toString() {
        return "CredencialesCreadasEvent[usuarioId=%d, nombreUsuario=%s, passwordTemporal=****]"
                .formatted(usuarioId, nombreUsuario);
    }
}
