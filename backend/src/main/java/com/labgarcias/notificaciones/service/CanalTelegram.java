package com.labgarcias.notificaciones.service;

import org.springframework.stereotype.Component;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.Notificacion;

/**
 * PATRÓN: Adapter (implementación de CanalNotificacion)
 * PROBLEMA: D-21 pone a Telegram entre los canales de RN-19 desde ya, pero su integración real
 *           depende de un bot que todavía no existe (P-20). Sin este adaptador, el despachador
 *           tendría que saber que Telegram "todavía no está", y el envío quedaría sin registro.
 * MOTIVADO POR: RN-19, D-21, §6.3.
 *
 * **Solo estructura (Plan.md T-21).** El canal real —Bot API, `sendMessage`, `chat_id`— es T-32,
 * junto con la vinculación de §6.5. Acá cada envío se marca FALLIDO con un motivo claro, que es
 * exactamente lo que hay que ver hasta que el bot exista. No integrar nada en esta tarea.
 */
@Component
public class CanalTelegram implements CanalNotificacion {

    private static final String SIN_CONFIGURAR = "Canal no configurado: la integración con Telegram llega en T-32.";

    @Override
    public boolean soporta(Canal canal) {
        return canal == Canal.TELEGRAM;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        throw new EnvioNoRealizadoException(SIN_CONFIGURAR);
    }
}
