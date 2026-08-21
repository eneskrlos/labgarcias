package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.Notificacion;
import com.labgarcias.notificaciones.domain.TipoEvento;
import com.labgarcias.seguridad.domain.Usuario;

/**
 * T-32: Telegram dejó de ser estructura. Estos tests fijan las cuatro cosas de §6.3 y §6.5 que
 * no se pueden perder: que el envío sale por la Bot API, que sin vinculación queda FALLIDO con
 * su motivo, que sin token el canal se deshabilita sin romper nada, y —criterio 4 de §6.5— que
 * **el token no aparece en ningún mensaje**, ni siquiera cuando la librería HTTP falla y lo
 * arrastra en la URL.
 *
 * `Usuario` se moquea porque las columnas de Telegram todavía no tienen setter: las escribe la
 * vinculación (T-32b) y acá solo se leen.
 */
class CanalTelegramTest {

    private static final String TOKEN = "123456789:AAG-token-secreto-del-bot";
    private static final String URL_ESPERADA = "https://api.telegram.org/bot" + TOKEN + "/sendMessage";
    private static final String CHAT_ID = "987654321";
    private static final String MENSAJE = "El trabajo del paciente Código 1000 pasó a la etapa de Listo.";
    private static final String RESPUESTA_OK = "{\"ok\":true,\"result\":{\"message_id\":1}}";

    private final RestClient.Builder clienteBuilder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(clienteBuilder).build();
    private final CanalTelegram canalTelegram = new CanalTelegram(clienteBuilder, TOKEN);

    private Notificacion notificacion(boolean vinculado, String chatId) {
        Usuario destinatario = mock(Usuario.class);
        when(destinatario.isTelegramVinculado()).thenReturn(vinculado);
        when(destinatario.getTelegramChatId()).thenReturn(chatId);
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

    /** §6.3: `POST /bot{token}/sendMessage` con `chat_id` y `text`. §6.5 criterio 1. */
    @Test
    void enviaPorLaBotApiConElChatDelUsuarioYElTextoDeLaNotificacion() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.chat_id").value(CHAT_ID))
                .andExpect(jsonPath("$.text").value(MENSAJE))
                .andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));

        canalTelegram.enviar(notificacion(true, CHAT_ID));

        servidor.verify();
    }

    /**
     * §6.3: sin `telegram_chat_id` el envío queda FALLIDO con "Telegram no vinculado" — y ni
     * siquiera se intenta la llamada. El `verify()` sobre un servidor sin expectativas es lo que
     * prueba que no se llamó a nadie.
     */
    @Test
    void sinVinculacionNoLlamaALaApiYFallaConElMotivoDocumentado() {
        assertThatThrownBy(() -> canalTelegram.enviar(notificacion(false, null)))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessage("Telegram no vinculado");

        servidor.verify();
    }

    /**
     * §6.5 criterio 3: desvincular detiene los envíos. Si quedara un `chat_id` viejo con la
     * bandera apagada, mandarle igual sería escribirle a alguien que pidió no recibir más.
     */
    @Test
    void conLaBanderaApagadaNoEnviaAunqueQuedeUnChatIdViejo() {
        assertThatThrownBy(() -> canalTelegram.enviar(notificacion(false, CHAT_ID)))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessage("Telegram no vinculado");

        servidor.verify();
    }

    /** §6.5: sin token configurado el canal se deshabilita; construirlo no puede romper el arranque. */
    @Test
    void sinTokenElCanalSeDeshabilitaSinRomperElArranque() {
        RestClient.Builder builderSinUso = RestClient.builder();
        CanalTelegram sinToken = new CanalTelegram(builderSinUso, "");

        assertThatCode(() -> sinToken.soporta(Canal.TELEGRAM)).doesNotThrowAnyException();
        assertThatThrownBy(() -> sinToken.enviar(notificacion(true, CHAT_ID)))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessageContaining("no configurado")
                .hasMessageContaining("telegram.bot.token");
    }

    /** Un rechazo de la Bot API tiene que llegar a `detalle_error` con el motivo que dio Telegram. */
    @Test
    void unRechazoDeLaApiSeTraduceAFalloDeEnvioConElMotivoDeTelegram() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"description\":\"Bad Request: chat not found\"}"));

        assertThatThrownBy(() -> canalTelegram.enviar(notificacion(true, CHAT_ID)))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessageContaining("chat not found");
    }

    /** La Bot API puede contestar 200 con `ok:false`. Un envío que no salió no puede quedar ENVIADO. */
    @Test
    void unaRespuestaConOkFalseNoSeDaPorEnviada() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withSuccess("{\"ok\":false,\"description\":\"Forbidden: bot was blocked by the user\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> canalTelegram.enviar(notificacion(true, CHAT_ID)))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessageContaining("bot was blocked");
    }

    /**
     * §6.5 criterio 4, el caso que se escapa solo: la excepción de red de Spring incluye la URI
     * completa, o sea el token. Si ese mensaje se propagara, el secreto terminaría guardado en
     * `notificacion_envio.detalle_error` y a la vista de cualquiera que abra la base.
     */
    @Test
    void criterio4NingunFalloDeRedFiltraElTokenDelBot() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withException(new IOException("I/O error on POST request for \"" + URL_ESPERADA + "\"")));

        assertThatThrownBy(() -> canalTelegram.enviar(notificacion(true, CHAT_ID)))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessageContaining("No se pudo contactar")
                .hasMessageNotContaining(TOKEN);
    }

    /** Lo mismo para el rechazo de la API: el motivo sirve, el token no puede viajar con él. */
    @Test
    void criterio4ElRechazoDeLaApiTampocoFiltraElToken() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"description\":\"Unauthorized\"}"));

        assertThatThrownBy(() -> canalTelegram.enviar(notificacion(true, CHAT_ID)))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessageNotContaining(TOKEN);
    }
}
