package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.labgarcias.notificaciones.service.ClienteTelegram.MensajeRecibido;

/**
 * Todo el trato con la Bot API. Lo que estos tests cuidan, además de que las dos llamadas
 * funcionen, es el criterio 4 de §6.5: **el token no puede aparecer en ningún mensaje**, ni
 * siquiera cuando la librería HTTP falla y lo arrastra dentro de la URL.
 */
class ClienteTelegramTest {

    private static final String TOKEN = "123456789:AAG-token-secreto-del-bot";
    private static final String BOT = "labgarcias_bot";
    private static final String URL_ENVIO = "https://api.telegram.org/bot" + TOKEN + "/sendMessage";
    private static final String URL_ACTUALIZACIONES = "https://api.telegram.org/bot" + TOKEN + "/getUpdates";
    private static final String CHAT_ID = "987654321";
    private static final String MENSAJE = "El trabajo del paciente Código 1000 pasó a la etapa de Listo.";

    private final RestClient.Builder clienteBuilder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(clienteBuilder).build();
    private final ClienteTelegram clienteTelegram = new ClienteTelegram(clienteBuilder, TOKEN, BOT);

    /** §6.5: sin token no se envía; sin nombre de bot no se puede armar el enlace de vinculación. */
    @Test
    void laConfiguracionDecideQueQuedaHabilitado() {
        assertThat(clienteTelegram.envioHabilitado()).isTrue();
        assertThat(clienteTelegram.vinculacionHabilitada()).isTrue();

        ClienteTelegram sinNombre = new ClienteTelegram(RestClient.builder(), TOKEN, "");
        assertThat(sinNombre.envioHabilitado()).isTrue();
        assertThat(sinNombre.vinculacionHabilitada()).isFalse();

        ClienteTelegram sinToken = new ClienteTelegram(RestClient.builder(), "", BOT);
        assertThat(sinToken.envioHabilitado()).isFalse();
        assertThat(sinToken.vinculacionHabilitada()).isFalse();
    }

    /** §6.5: construirlo sin configuración no puede romper el arranque. */
    @Test
    void sinConfiguracionSeConstruyeIgual() {
        assertThatCode(() -> new ClienteTelegram(RestClient.builder(), "", ""))
                .doesNotThrowAnyException();
    }

    /** §6.3: `POST /bot{token}/sendMessage` con `chat_id` y `text`. */
    @Test
    void enviaPorLaBotApiConElChatYElTextoIndicados() {
        servidor.expect(requestTo(URL_ENVIO))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.chat_id").value(CHAT_ID))
                .andExpect(jsonPath("$.text").value(MENSAJE))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        clienteTelegram.enviarMensaje(CHAT_ID, MENSAJE);

        servidor.verify();
    }

    @Test
    void unRechazoDeLaApiSeTraduceAFalloDeEnvioConElMotivoDeTelegram() {
        servidor.expect(requestTo(URL_ENVIO))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"description\":\"Bad Request: chat not found\"}"));

        assertThatThrownBy(() -> clienteTelegram.enviarMensaje(CHAT_ID, MENSAJE))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessageContaining("chat not found")
                .hasMessageNotContaining(TOKEN);
    }

    /** La Bot API puede contestar 200 con `ok:false`. Un envío que no salió no puede darse por hecho. */
    @Test
    void unaRespuestaConOkFalseNoSeDaPorEnviada() {
        servidor.expect(requestTo(URL_ENVIO))
                .andRespond(withSuccess("{\"ok\":false,\"description\":\"Forbidden: bot was blocked by the user\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> clienteTelegram.enviarMensaje(CHAT_ID, MENSAJE))
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
        servidor.expect(requestTo(URL_ENVIO))
                .andRespond(withException(new IOException("I/O error on POST request for \"" + URL_ENVIO + "\"")));

        assertThatThrownBy(() -> clienteTelegram.enviarMensaje(CHAT_ID, MENSAJE))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessageContaining("No se pudo contactar")
                .hasMessageNotContaining(TOKEN);
    }

    /** §6.5 paso 4: `getUpdates` con el offset, y de cada novedad solo interesan chat y texto. */
    @Test
    void leeLasActualizacionesDesdeElOffsetIndicado() {
        servidor.expect(requestTo(URL_ACTUALIZACIONES + "?offset=42"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"ok":true,"result":[
                          {"update_id":42,"message":{"chat":{"id":987654321},"text":"/start abc123"}}
                        ]}""", MediaType.APPLICATION_JSON));

        List<MensajeRecibido> mensajes = clienteTelegram.obtenerActualizaciones(42);

        assertThat(mensajes).singleElement().satisfies(mensaje -> {
            assertThat(mensaje.actualizacionId()).isEqualTo(42);
            assertThat(mensaje.chatId()).isEqualTo(CHAT_ID);
            assertThat(mensaje.texto()).isEqualTo("/start abc123");
        });
    }

    /**
     * Las novedades que no son un mensaje de texto también vienen: quien las consume necesita
     * verlas para avanzar el offset. Si se filtraran acá, Telegram las entregaría para siempre.
     */
    @Test
    void lasNovedadesQueNoSonMensajeVienenIgualParaPoderConfirmarlas() {
        servidor.expect(requestTo(URL_ACTUALIZACIONES + "?offset=0"))
                .andRespond(withSuccess("""
                        {"ok":true,"result":[
                          {"update_id":7,"edited_message":{"chat":{"id":1},"text":"editado"}}
                        ]}""", MediaType.APPLICATION_JSON));

        assertThat(clienteTelegram.obtenerActualizaciones(0)).singleElement().satisfies(mensaje -> {
            assertThat(mensaje.actualizacionId()).isEqualTo(7);
            assertThat(mensaje.chatId()).isNull();
            assertThat(mensaje.texto()).isNull();
        });
    }

    /**
     * Un fallo leyendo actualizaciones no tiene a quién reportarse: lo llama un proceso
     * programado. Se devuelve vacío y la próxima corrida vuelve a intentar.
     */
    @Test
    void unFalloLeyendoActualizacionesDevuelveVacioYNoPropaga() {
        servidor.expect(requestTo(URL_ACTUALIZACIONES + "?offset=0"))
                .andRespond(withException(new IOException("I/O error")));

        assertThat(clienteTelegram.obtenerActualizaciones(0)).isEmpty();
    }
}
