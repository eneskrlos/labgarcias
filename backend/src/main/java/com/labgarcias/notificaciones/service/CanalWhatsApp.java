package com.labgarcias.notificaciones.service;

import org.springframework.stereotype.Component;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.Notificacion;
import com.labgarcias.seguridad.domain.Usuario;

/**
 * PATRÓN: Adapter (implementación de CanalNotificacion)
 * PROBLEMA: V2 dejó la columna `canal_whatsapp_activo` y el valor 'WHATSAPP' en chk_envio_canal.
 *           Si alguien activa la bandera y no existe un adaptador que la atienda, el despachador
 *           se queda con un envío que ningún canal soporta.
 * MOTIVADO POR: P-18, D-21, §6.3, Agente.md 5.5 (adaptador del puerto ya autorizado).
 *
 * **Solo estructura (P-18), también después de T-32.** WhatsApp exige un proveedor pago (Meta
 * Cloud API o Twilio) que la clienta no contrató; D-21 lo reemplazó por Telegram, que es el que
 * T-32 integró de verdad. Acá no hay ni cliente HTTP ni credenciales: §6.3 pide exactamente tres
 * cosas —implementar el puerto, validar el teléfono y marcar FALLIDO con "canal no configurado"—
 * y eso es todo lo que hace. Ninguna configuración lo activa hoy: el conjunto por defecto de §6.3
 * no lo incluye y la columna nace en FALSE.
 */
@Component
public class CanalWhatsApp implements CanalNotificacion {

    private static final String SIN_TELEFONO = "WhatsApp sin destino: el usuario no tiene teléfono.";
    private static final String SIN_CONFIGURAR = "Canal no configurado: WhatsApp requiere un proveedor externo (P-18).";

    @Override
    public boolean soporta(Canal canal) {
        return canal == Canal.WHATSAPP;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        Usuario destinatario = notificacion.getDestinatario();
        // §6.3 pide validar el teléfono aunque no haya proveedor: el día que se contrate uno, el
        // motivo por el que un envío no salió tiene que distinguir "falta el destino" de "falta el
        // proveedor". Son dos problemas con dos responsables distintos.
        if (destinatario.getTelefono() == null || destinatario.getTelefono().isBlank()) {
            throw new EnvioNoRealizadoException(SIN_TELEFONO);
        }
        throw new EnvioNoRealizadoException(SIN_CONFIGURAR);
    }
}
