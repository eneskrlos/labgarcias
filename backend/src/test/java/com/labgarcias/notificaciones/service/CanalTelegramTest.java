package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.Notificacion;
import com.labgarcias.notificaciones.domain.TipoEvento;
import com.labgarcias.seguridad.domain.Usuario;

/**
 * El canal, sin HTTP: el trato con la Bot API está en `ClienteTelegramTest`. Acá se fija lo que
 * decide el canal — a quién se le manda y cuándo no se manda —, que es donde viven los criterios
 * de §6.3 y el criterio 3 de §6.5.
 */
class CanalTelegramTest {

    private static final String CHAT_ID = "987654321";
    private static final String MENSAJE = "El trabajo del paciente Código 1000 pasó a la etapa de Listo.";

    private final ClienteTelegram clienteTelegram = mock(ClienteTelegram.class);
    private final CanalTelegram canalTelegram = new CanalTelegram(clienteTelegram);

    private Notificacion notificacion(boolean vinculado, String chatId) {
        Usuario destinatario = new Usuario();
        if (vinculado) {
            destinatario.vincularTelegram(chatId);
        } else if (chatId != null) {
            // Chat cargado con la bandera apagada: es el estado que deja una desvinculación a
            // medias, y el que tiene que seguir sin enviar (§6.5 criterio 3).
            ReflectionTestUtils.setField(destinatario, "telegramChatId", chatId);
        }
        Notificacion notificacion = new Notificacion();
        notificacion.setDestinatario(destinatario);
        notificacion.setTipoEvento(TipoEvento.CAMBIO_ESTADO);
        notificacion.setMensaje(MENSAJE);
        return notificacion;
    }

    @Test
    void atiendeSoloElCanalTelegram() {
        assertThat(canalTelegram.soporta(Canal.TELEGRAM)).isTrue();
        assertThat(canalTelegram.soporta(Canal.WHATSAPP)).isFalse();
        assertThat(canalTelegram.soporta(Canal.CORREO)).isFalse();
    }

    @Test
    void leEnviaAlChatDelDestinatarioElTextoDeLaNotificacion() {
        when(clienteTelegram.envioHabilitado()).thenReturn(true);

        canalTelegram.enviar(notificacion(true, CHAT_ID));

        verify(clienteTelegram).enviarMensaje(CHAT_ID, MENSAJE);
    }

    /** §6.3: sin vinculación el envío queda FALLIDO con su motivo, y ni se intenta la llamada. */
    @Test
    void sinVinculacionNoLlamaALaApiYFallaConElMotivoDocumentado() {
        when(clienteTelegram.envioHabilitado()).thenReturn(true);

        assertThatThrownBy(() -> canalTelegram.enviar(notificacion(false, null)))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessage("Telegram no vinculado");

        verify(clienteTelegram, never()).enviarMensaje(anyString(), anyString());
    }

    /**
     * §6.5 criterio 3: desvincular detiene los envíos. Si quedara un `chat_id` viejo con la
     * bandera apagada, mandarle igual sería escribirle a alguien que pidió no recibir más.
     */
    @Test
    void conLaBanderaApagadaNoEnviaAunqueQuedeUnChatIdViejo() {
        when(clienteTelegram.envioHabilitado()).thenReturn(true);

        assertThatThrownBy(() -> canalTelegram.enviar(notificacion(false, CHAT_ID)))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessage("Telegram no vinculado");

        verify(clienteTelegram, never()).enviarMensaje(anyString(), anyString());
    }

    /** §6.5: sin token configurado el canal se deshabilita y lo dice con claridad. */
    @Test
    void sinTokenConfiguradoElCanalNoEnviaYLoDiceConClaridad() {
        when(clienteTelegram.envioHabilitado()).thenReturn(false);

        assertThatThrownBy(() -> canalTelegram.enviar(notificacion(true, CHAT_ID)))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessageContaining("no configurado")
                .hasMessageContaining("telegram.bot.token");

        verify(clienteTelegram, never()).enviarMensaje(any(), any());
    }
}
