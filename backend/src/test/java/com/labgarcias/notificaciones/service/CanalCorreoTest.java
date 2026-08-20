package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.Notificacion;
import com.labgarcias.notificaciones.domain.TipoEvento;
import com.labgarcias.seguridad.domain.Usuario;

class CanalCorreoTest {

    private static final String REMITENTE = "no-responder@labgarcias.local";
    private static final String MENSAJE = "El trabajo del paciente Código 1000 pasó a la etapa de Listo.";

    private final JavaMailSender remitenteCorreo = mock(JavaMailSender.class);
    private final CanalCorreo canalCorreo = new CanalCorreo(remitenteCorreo, REMITENTE);

    private Notificacion notificacion() {
        Usuario destinatario = new Usuario();
        destinatario.setCorreo("juan@mail.com");
        Notificacion notificacion = new Notificacion();
        notificacion.setDestinatario(destinatario);
        notificacion.setTipoEvento(TipoEvento.CAMBIO_ESTADO);
        notificacion.setMensaje(MENSAJE);
        return notificacion;
    }

    @Test
    void atiendeSoloElCanalCorreo() {
        assertThat(canalCorreo.soporta(Canal.CORREO)).isTrue();
        assertThat(canalCorreo.soporta(Canal.APP)).isFalse();
        assertThat(canalCorreo.soporta(Canal.TELEGRAM)).isFalse();
    }

    @Test
    void escribeAlDestinatarioConElAsuntoDelEventoYElMensajeDeLaNotificacion() {
        canalCorreo.enviar(notificacion());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(remitenteCorreo).send(captor.capture());
        SimpleMailMessage correo = captor.getValue();
        assertThat(correo.getTo()).containsExactly("juan@mail.com");
        assertThat(correo.getFrom()).isEqualTo(REMITENTE);
        assertThat(correo.getSubject()).isNotBlank();
        assertThat(correo.getText()).isEqualTo(MENSAJE);
    }

    /**
     * §6 criterio 2: el SMTP caído se traduce a un fallo de envío con su motivo, no a un error
     * del sistema. Que suba como EnvioNoRealizadoException es lo que deja el envío en FALLIDO
     * en vez de propagarse como un 500.
     */
    @Test
    void criterio2UnSmtpCaidoSeTraduceAFalloDeEnvioConSuMotivo() {
        doThrow(new MailSendException("Connection refused")).when(remitenteCorreo).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> canalCorreo.enviar(notificacion()))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessageContaining("Connection refused");
    }
}
