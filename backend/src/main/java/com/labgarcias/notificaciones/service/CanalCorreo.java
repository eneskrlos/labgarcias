package com.labgarcias.notificaciones.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.Notificacion;
import com.labgarcias.seguridad.domain.Usuario;

/**
 * PATRÓN: Adapter (implementación de CanalNotificacion)
 * PROBLEMA: el envío por SMTP depende de un servidor externo que puede estar caído, mal
 *           configurado o rechazar la dirección. Aislarlo detrás del puerto permite que ese
 *           fallo se registre como un envío FALLIDO en vez de romper la operación que lo originó.
 * MOTIVADO POR: RN-05 y D-20 (el cambio de estado avisa por correo), §6.3 (SMTP por properties).
 */
@Component
public class CanalCorreo implements CanalNotificacion {

    private final JavaMailSender remitenteCorreo;
    private final String remitente;

    public CanalCorreo(JavaMailSender remitenteCorreo,
                       @Value("${app.notificaciones.correo.remitente}") String remitente) {
        this.remitenteCorreo = remitenteCorreo;
        this.remitente = remitente;
    }

    @Override
    public boolean soporta(Canal canal) {
        return canal == Canal.CORREO;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        Usuario destinatario = notificacion.getDestinatario();
        enviarCorreo(destinatario.getCorreo(),
                TextosNotificacion.asuntoDe(notificacion.getTipoEvento()),
                notificacion.getMensaje());
    }

    /**
     * §3.1.b: envío que no sale del outbox. El correo de credenciales lleva la contraseña
     * temporal, que no puede persistirse en `notificacion.mensaje`, así que su cuerpo se compone
     * en memoria y se manda por acá.
     *
     * Público para que ese caso no tenga que abrir su propia conexión SMTP: el servidor de correo
     * se configura y se maneja en un solo lugar.
     */
    public void enviarCorreo(String destino, String asunto, String cuerpo) {
        SimpleMailMessage correo = new SimpleMailMessage();
        correo.setFrom(remitente);
        correo.setTo(destino);
        correo.setSubject(asunto);
        correo.setText(cuerpo);
        try {
            remitenteCorreo.send(correo);
        } catch (MailException ex) {
            // §6 criterio 2: el motivo tiene que llegar a detalle_error para poder diagnosticar,
            // pero no puede subir como error del sistema: la notificación ya está a salvo.
            throw new EnvioNoRealizadoException(ex.getMessage());
        }
    }
}
