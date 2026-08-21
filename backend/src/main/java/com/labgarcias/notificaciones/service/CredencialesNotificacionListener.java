package com.labgarcias.notificaciones.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.TipoEvento;
import com.labgarcias.seguridad.domain.CredencialesCreadasEvent;

/**
 * PATRÓN: Observer
 * PROBLEMA: el correo con la contraseña temporal no puede salir por el outbox —persistiría la
 *           contraseña en claro en `notificacion.mensaje`— pero tampoco puede enviarse dentro de
 *           la transacción del alta, porque un SMTP lento o caído voltearía la creación de la
 *           cuenta.
 * MOTIVADO POR: D-18/§3.1.b paso 4 y su "tratamiento de la contraseña temporal", Agente.md 5.6.
 *
 * La contraseña llega en el evento, se usa para componer el cuerpo y muere con el método: no se
 * persiste ni se loguea en ningún punto.
 *
 * **Limitación aceptada (§3.1.b):** si el envío falla, el envío queda FALLIDO y *no es
 * reintentable* —el despachador solo toma PENDIENTE—, porque la contraseña ya no existe en
 * ningún lado. El remedio es volver a crear las credenciales.
 */
@Component
public class CredencialesNotificacionListener {

    private static final String CUERPO_CORREO = """
            Se creó tu cuenta en Lab. Garcia's Connect.

            Usuario: %s
            Contraseña temporal: %s

            Por seguridad, el sistema te va a pedir que la cambies la primera vez que ingreses.""";

    private final NotificacionService notificacionService;
    private final CanalCorreo canalCorreo;

    public CredencialesNotificacionListener(NotificacionService notificacionService, CanalCorreo canalCorreo) {
        this.notificacionService = notificacionService;
        this.canalCorreo = canalCorreo;
    }

    /**
     * §6.2: `CREDENCIALES_CREADAS` va al odontólogo y **solo por correo** — en este momento
     * todavía no vinculó Telegram, así que es el único canal garantizado.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCrearseLasCredenciales(CredencialesCreadasEvent evento) {
        String detalleError = enviarCredenciales(evento);
        notificacionService.registrarConEnvioResuelto(
                evento.usuarioId(),
                TipoEvento.CREDENCIALES_CREADAS,
                TextosNotificacion.credencialesCreadas(),
                Canal.CORREO,
                detalleError);
    }

    /** Devuelve null si salió bien, o el motivo del fallo para `detalle_error`. */
    private String enviarCredenciales(CredencialesCreadasEvent evento) {
        try {
            canalCorreo.enviarCorreo(
                    evento.correo(),
                    TextosNotificacion.asuntoDe(TipoEvento.CREDENCIALES_CREADAS),
                    CUERPO_CORREO.formatted(evento.nombreUsuario(), evento.passwordTemporal()));
            return null;
        } catch (EnvioNoRealizadoException excepcion) {
            // El motivo del fallo del SMTP no contiene la contraseña: es el mensaje del servidor.
            return excepcion.getMessage();
        }
    }
}
