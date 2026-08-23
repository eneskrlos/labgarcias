package com.labgarcias.notificaciones.service;

import org.springframework.stereotype.Component;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.Notificacion;
import com.labgarcias.seguridad.domain.Usuario;

/**
 * PATRÓN: Adapter (implementación de CanalNotificacion)
 * PROBLEMA: entregar por Telegram es una llamada HTTP a un servicio de terceros que puede tardar,
 *           rechazar el destino o estar caído, y que además exige un secreto (el token del bot).
 *           Aislarlo detrás del puerto deja ese riesgo fuera del despachador, que sigue
 *           registrando un envío FALLIDO con su motivo sin saber que del otro lado hay HTTP.
 * MOTIVADO POR: D-21, §6.3 (Bot API, `sendMessage`, token en properties), §6.5 criterio 4.
 *
 * El trato con la API vive en `ClienteTelegram`, que comparte con la vinculación de §6.5. Acá
 * queda lo que es del canal: **a quién** se le manda y **cuándo no se manda**.
 *
 * El destino es `usuario.telegram_chat_id` (D-21, §6.3), que puebla la vinculación de §6.5. La
 * otra columna, `configuracion_notificacion.telegram_chat_id`, es el chat del laboratorio y no
 * interviene acá: ver la deuda de spec anotada en ESTADO.md.
 */
@Component
public class CanalTelegram implements CanalNotificacion {

    private static final String SIN_TOKEN =
            "Canal no configurado: falta telegram.bot.token (P-20, crear el bot con @BotFather).";
    private static final String SIN_VINCULAR = "Telegram no vinculado";

    private final ClienteTelegram clienteTelegram;

    public CanalTelegram(ClienteTelegram clienteTelegram) {
        this.clienteTelegram = clienteTelegram;
    }

    @Override
    public boolean soporta(Canal canal) {
        return canal == Canal.TELEGRAM;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        if (!clienteTelegram.envioHabilitado()) {
            throw new EnvioNoRealizadoException(SIN_TOKEN);
        }
        Usuario destinatario = notificacion.getDestinatario();
        if (!estaVinculado(destinatario)) {
            // §6.3: no es un error del sistema, es un destinatario que todavía no se vinculó.
            throw new EnvioNoRealizadoException(SIN_VINCULAR);
        }
        clienteTelegram.enviarMensaje(destinatario.getTelegramChatId(), notificacion.getMensaje());
    }

    /**
     * §6.5 criterio 3: desvincular tiene que detener los envíos. Se exige la bandera además del
     * chat porque un chat viejo con la bandera apagada sería escribirle a alguien que pidió no
     * recibir más.
     */
    private boolean estaVinculado(Usuario destinatario) {
        return destinatario.isTelegramVinculado()
                && destinatario.getTelegramChatId() != null
                && !destinatario.getTelegramChatId().isBlank();
    }
}
