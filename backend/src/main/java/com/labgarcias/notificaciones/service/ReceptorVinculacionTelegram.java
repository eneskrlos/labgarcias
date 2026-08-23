package com.labgarcias.notificaciones.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.labgarcias.notificaciones.service.ClienteTelegram.MensajeRecibido;

/**
 * §6.5 paso 4: lo que el bot recibe. Se consulta por `getUpdates` con `@Scheduled`, **nunca por
 * webhook** (D-16): un webhook exige HTTPS público y complicaría la instalación de cada
 * laboratorio.
 *
 * El `offset` vive en memoria a propósito. Pedir las novedades con un offset **confirma** las
 * anteriores contra Telegram, que es lo que hace que no se vuelvan a entregar; guardarlo además
 * en la base sería una columna nueva —una migración— para un dato que el propio Telegram ya
 * retiene. Tras un reinicio se releen las novedades sin confirmar, y eso no rompe nada: el token
 * es de un solo uso, así que un `/start` repetido no vincula dos veces.
 */
@Component
public class ReceptorVinculacionTelegram {

    private static final Logger log = LoggerFactory.getLogger(ReceptorVinculacionTelegram.class);

    private static final String COMANDO_INICIO = "/start ";

    /** §6.5 paso 4: texto documentado palabra por palabra. No cambiarlo sin cambiar la spec. */
    private static final String VINCULACION_EXITOSA =
            "✅ Cuenta vinculada. Vas a recibir las notificaciones del laboratorio por acá.";

    /** §6.5 criterio 2: el bot responde el error. Sin texto documentado: calcado del anterior. */
    private static final String VINCULACION_RECHAZADA =
            "No pudimos vincular la cuenta: el enlace ya se usó o venció. "
                    + "Pedí uno nuevo desde tu perfil en Lab. Garcia's Connect.";

    private final ClienteTelegram clienteTelegram;
    private final VinculacionTelegramService vinculacionTelegramService;

    private long proximoOffset;

    public ReceptorVinculacionTelegram(ClienteTelegram clienteTelegram,
                                       VinculacionTelegramService vinculacionTelegramService) {
        this.clienteTelegram = clienteTelegram;
        this.vinculacionTelegramService = vinculacionTelegramService;
    }

    @Scheduled(fixedDelayString = "${telegram.polling-ms}")
    public void recibir() {
        // §6.5: sin bot configurado la vinculación queda deshabilitada, y sin esto el proceso
        // programado golpearía a la API cada pocos segundos con una URL sin token.
        if (!clienteTelegram.vinculacionHabilitada()) {
            return;
        }
        for (MensajeRecibido mensaje : clienteTelegram.obtenerActualizaciones(proximoOffset)) {
            proximoOffset = mensaje.actualizacionId() + 1;
            atender(mensaje);
        }
    }

    /** Un mensaje que falla no puede cortar la tanda ni repetirse para siempre: el offset ya avanzó. */
    private void atender(MensajeRecibido mensaje) {
        String token = tokenDe(mensaje);
        if (token == null) {
            return;
        }
        try {
            boolean vinculado = vinculacionTelegramService.vincular(token, mensaje.chatId());
            clienteTelegram.enviarMensaje(mensaje.chatId(),
                    vinculado ? VINCULACION_EXITOSA : VINCULACION_RECHAZADA);
        } catch (RuntimeException ex) {
            // Ni el token ni el texto del mensaje entran al log; los mensajes del cliente ya vienen
            // sin el token del bot (§6.5 criterio 4).
            log.error("No se pudo atender un mensaje del bot de Telegram", ex);
        }
    }

    /**
     * §6.5 solo define `/start {token}`. Cualquier otra cosa que le escriban al bot se ignora en
     * silencio: inventarle respuestas sería inventar conversación que la spec no define.
     */
    private String tokenDe(MensajeRecibido mensaje) {
        if (mensaje.chatId() == null || mensaje.texto() == null
                || !mensaje.texto().startsWith(COMANDO_INICIO)) {
            return null;
        }
        String token = mensaje.texto().substring(COMANDO_INICIO.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
