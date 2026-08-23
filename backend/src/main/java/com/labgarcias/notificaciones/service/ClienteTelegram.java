package com.labgarcias.notificaciones.service;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Todo el trato con la Bot API de Telegram pasa por acá: `sendMessage` (§6.3) y `getUpdates`
 * (§6.5). Es una sola clase y no dos porque las dos llamadas comparten lo que no se puede
 * equivocar —el token y la traducción de errores— y duplicarlo sería duplicar el riesgo.
 *
 * **El token no sale de esta clase** (§6.5 criterio 4). Viaja en la URL, así que ningún mensaje
 * de la librería HTTP se propaga tal cual: `ResourceAccessException` incluye la URI completa y
 * terminaría copiada en `notificacion_envio.detalle_error` o en un log. Cada fallo se traduce a
 * un texto propio.
 *
 * **Sin configuración no rompe nada** (§6.5): sin token no se envía y sin nombre de bot no se
 * puede vincular, pero la aplicación arranca igual y cada caso avisa con su motivo.
 */
@Component
public class ClienteTelegram {

    private static final Logger log = LoggerFactory.getLogger(ClienteTelegram.class);

    private static final String URL_BASE = "https://api.telegram.org";

    private static final String RECHAZO_API = "Telegram rechazó el envío: %s";
    private static final String SIN_MOTIVO = "sin motivo informado (HTTP %d)";
    private static final String INALCANZABLE = "No se pudo contactar con la API de Telegram.";

    private final RestClient cliente;
    /** URL del bot, token incluido. No loguear, no propagar. */
    private final String urlBot;
    private final String nombreBot;

    public ClienteTelegram(RestClient.Builder clienteBuilder,
                           @Value("${telegram.bot.token:}") String token,
                           @Value("${telegram.bot.username:}") String nombreBot) {
        this.cliente = clienteBuilder.build();
        this.nombreBot = nombreBot == null ? "" : nombreBot.trim();
        this.urlBot = token == null || token.isBlank() ? null : URL_BASE + "/bot" + token;
        registrarConfiguracion();
    }

    /** §6.3: sin token no hay envío posible. */
    public boolean envioHabilitado() {
        return urlBot != null;
    }

    /** §6.5: el enlace profundo necesita además el nombre del bot; sin él no hay vinculación. */
    public boolean vinculacionHabilitada() {
        return envioHabilitado() && !nombreBot.isBlank();
    }

    public String getNombreBot() {
        return nombreBot;
    }

    /** §6.3: un mensaje al chat indicado. Falla con un motivo apto para `detalle_error`. */
    public void enviarMensaje(String chatId, String texto) {
        try {
            RespuestaEnvio respuesta = cliente.post()
                    .uri(URI.create(urlBot + "/sendMessage"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", chatId, "text", texto))
                    .retrieve()
                    .body(RespuestaEnvio.class);
            // La Bot API puede contestar 200 con ok=false. Un envío que no salió no puede quedar ENVIADO.
            if (respuesta == null || !respuesta.ok()) {
                throw new EnvioNoRealizadoException(RECHAZO_API.formatted(descripcionDe(respuesta)));
            }
        } catch (RestClientResponseException ex) {
            // El cuerpo del error lo escribe Telegram ("Bad Request: chat not found") y sirve para
            // diagnosticar; la excepción entera, no: arrastra la URL con el token.
            throw new EnvioNoRealizadoException(RECHAZO_API.formatted(descripcionDe(ex)));
        } catch (RestClientException ex) {
            // Red, DNS o timeout. El mensaje original lleva la URI: se descarta a propósito.
            throw new EnvioNoRealizadoException(INALCANZABLE);
        }
    }

    /**
     * §6.5 paso 4: las novedades que el bot recibió, desde `offset`. Pedirlas con un offset
     * **confirma** las anteriores, que es lo que hace que Telegram no vuelva a entregarlas.
     *
     * Un fallo acá no tiene a quién reportarse —lo llama un proceso programado, no un usuario—,
     * así que se registra y se devuelve vacío: la próxima corrida vuelve a intentar.
     */
    public List<MensajeRecibido> obtenerActualizaciones(long offset) {
        try {
            RespuestaActualizaciones respuesta = cliente.get()
                    .uri(URI.create(urlBot + "/getUpdates?offset=" + offset))
                    .retrieve()
                    .body(RespuestaActualizaciones.class);
            if (respuesta == null || !respuesta.ok() || respuesta.result() == null) {
                return List.of();
            }
            // Vienen todas, incluso las que no son un mensaje de texto: quien las consume necesita
            // verlas para avanzar el offset, o Telegram las volvería a entregar para siempre.
            return respuesta.result().stream().map(Actualizacion::aMensajeRecibido).toList();
        } catch (RestClientResponseException ex) {
            log.warn("Telegram rechazó la lectura de actualizaciones: {}", descripcionDe(ex));
            return List.of();
        } catch (RestClientException ex) {
            log.warn(INALCANZABLE);
            return List.of();
        }
    }

    private void registrarConfiguracion() {
        if (!envioHabilitado()) {
            log.warn("Telegram deshabilitado: no hay telegram.bot.token configurado. Los envíos por "
                    + "Telegram quedarán FALLIDO y la vinculación no está disponible.");
        } else if (!vinculacionHabilitada()) {
            log.warn("Telegram envía, pero la vinculación está deshabilitada: falta telegram.bot.username, "
                    + "que es lo que arma el enlace al bot.");
        } else {
            log.info("Telegram habilitado, envío y vinculación.");
        }
    }

    private String descripcionDe(RespuestaEnvio respuesta) {
        return respuesta == null || respuesta.description() == null
                ? SIN_MOTIVO.formatted(200)
                : respuesta.description();
    }

    private String descripcionDe(RestClientResponseException ex) {
        try {
            RespuestaEnvio cuerpo = ex.getResponseBodyAs(RespuestaEnvio.class);
            return cuerpo == null || cuerpo.description() == null
                    ? SIN_MOTIVO.formatted(ex.getStatusCode().value())
                    : cuerpo.description();
        } catch (RuntimeException noEsJson) {
            return SIN_MOTIVO.formatted(ex.getStatusCode().value());
        }
    }

    /**
     * Lo único que este sistema necesita de una novedad: quién escribió, qué escribió y su orden.
     * `chatId` y `texto` vienen en null cuando la novedad no es un mensaje de texto.
     */
    public record MensajeRecibido(long actualizacionId, String chatId, String texto) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaEnvio(boolean ok, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaActualizaciones(boolean ok, List<Actualizacion> result) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Actualizacion(@JsonProperty("update_id") long updateId, Mensaje message) {

        /** El bot puede recibir novedades que no son un mensaje de texto (§6.5 solo usa `/start`). */
        MensajeRecibido aMensajeRecibido() {
            boolean esMensaje = message != null && message.chat() != null;
            return new MensajeRecibido(updateId,
                    esMensaje ? String.valueOf(message.chat().id()) : null,
                    esMensaje ? message.text() : null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Mensaje(Chat chat, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Chat(Long id) {
    }
}
