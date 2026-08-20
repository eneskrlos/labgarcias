package com.labgarcias.notificaciones.service;

import org.springframework.stereotype.Component;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.Notificacion;

/**
 * PATRÓN: Adapter (implementación de CanalNotificacion)
 * PROBLEMA: V2 dejó la columna `canal_whatsapp_activo` y el valor 'WHATSAPP' en chk_envio_canal.
 *           Si alguien activa la bandera y no existe un adaptador que la atienda, el despachador
 *           se queda con un envío que ningún canal soporta.
 * MOTIVADO POR: P-18, D-21, §6.3, Agente.md 5.5 (adaptador del puerto ya autorizado).
 *
 * **Solo estructura (P-18).** WhatsApp exige un proveedor pago (Meta Cloud API o Twilio) que la
 * clienta no contrató; D-21 lo reemplazó por Telegram. Ninguna configuración lo activa hoy: el
 * conjunto por defecto de §6.3 no lo incluye y la columna nace en FALSE.
 */
@Component
public class CanalWhatsApp implements CanalNotificacion {

    private static final String SIN_CONFIGURAR = "Canal no configurado: WhatsApp requiere un proveedor externo (P-18).";

    @Override
    public boolean soporta(Canal canal) {
        return canal == Canal.WHATSAPP;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        throw new EnvioNoRealizadoException(SIN_CONFIGURAR);
    }
}
