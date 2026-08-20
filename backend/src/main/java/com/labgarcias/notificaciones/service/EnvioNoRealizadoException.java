package com.labgarcias.notificaciones.service;

/**
 * Un canal no pudo entregar la notificación por un motivo previsto —está sin configurar, o el
 * destinatario no tiene destino en él—. No es un error del sistema y no llega nunca al cliente
 * HTTP: el despachador lo traduce a `estado_envio = FALLIDO` con su mensaje en `detalle_error`.
 */
public class EnvioNoRealizadoException extends RuntimeException {

    public EnvioNoRealizadoException(String mensaje) {
        super(mensaje);
    }
}
