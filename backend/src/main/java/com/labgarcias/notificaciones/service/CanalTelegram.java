package com.labgarcias.notificaciones.service;

import java.net.URI;
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
import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.Notificacion;
import com.labgarcias.seguridad.domain.Usuario;

/**
 * PATRÓN: Adapter (implementación de CanalNotificacion)
 * PROBLEMA: entregar por Telegram es una llamada HTTP a un servicio de terceros que puede tardar,
 *           rechazar el destino o estar caído, y que además exige un secreto (el token del bot).
 *           Aislarlo detrás del puerto deja ese riesgo en un solo archivo: el despachador sigue
 *           registrando un envío FALLIDO con su motivo, sin saber que del otro lado hay HTTP.
 * MOTIVADO POR: D-21, §6.3 (Bot API, `sendMessage`, token en properties), §6.5 criterio 4.
 *
 * Tres reglas que no se pueden aflojar:
 *
 * 1. **El token nunca sale de acá** (§6.5 criterio 4). Viaja en la URL, así que ningún mensaje de
 *    error de la librería HTTP puede propagarse tal cual: `ResourceAccessException` incluye la URI
 *    completa y terminaría copiada en `notificacion_envio.detalle_error` y en el log. Por eso cada
 *    fallo se traduce a un texto propio.
 * 2. **Sin token, el canal se deshabilita, no rompe el arranque** (§6.5). Una instalación sin bot
 *    (P-20) tiene que levantar igual; lo que se ve es un envío FALLIDO con el motivo.
 * 3. **El destino es `usuario.telegram_chat_id`** (D-21, §6.3), poblado por la vinculación de §6.5
 *    —que es T-32b—. La otra columna, `configuracion_notificacion.telegram_chat_id`, es el chat del
 *    laboratorio y no interviene acá: ver la deuda de spec anotada en ESTADO.md.
 */
@Component
public class CanalTelegram implements CanalNotificacion {

    private static final Logger log = LoggerFactory.getLogger(CanalTelegram.class);

    private static final String URL_BASE = "https://api.telegram.org";

    private static final String SIN_TOKEN =
            "Canal no configurado: falta telegram.bot.token (P-20, crear el bot con @BotFather).";
    private static final String SIN_VINCULAR = "Telegram no vinculado";
    private static final String RECHAZO_API = "Telegram rechazó el envío: %s";
    private static final String SIN_MOTIVO = "sin motivo informado (HTTP %d)";
    private static final String INALCANZABLE = "No se pudo contactar con la API de Telegram.";

    private final RestClient cliente;
    /** URL completa de `sendMessage`, token incluido. No loguear, no propagar. */
    private final String urlEnvio;
    private final boolean habilitado;

    public CanalTelegram(RestClient.Builder clienteBuilder,
                         @Value("${telegram.bot.token:}") String token) {
        this.cliente = clienteBuilder.build();
        this.habilitado = token != null && !token.isBlank();
        this.urlEnvio = habilitado ? URL_BASE + "/bot" + token + "/sendMessage" : null;
        if (habilitado) {
            log.info("Canal Telegram habilitado.");
        } else {
            log.warn("Canal Telegram deshabilitado: no hay telegram.bot.token configurado. "
                    + "Los envíos por Telegram quedarán FALLIDO hasta que se configure.");
        }
    }

    @Override
    public boolean soporta(Canal canal) {
        return canal == Canal.TELEGRAM;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        if (!habilitado) {
            throw new EnvioNoRealizadoException(SIN_TOKEN);
        }
        Usuario destinatario = notificacion.getDestinatario();
        if (!destinatario.isTelegramVinculado() || destinatario.getTelegramChatId() == null
                || destinatario.getTelegramChatId().isBlank()) {
            // §6.3: no es un error del sistema, es un destinatario que todavía no se vinculó.
            throw new EnvioNoRealizadoException(SIN_VINCULAR);
        }
        enviarMensaje(destinatario.getTelegramChatId(), notificacion.getMensaje());
    }

    private void enviarMensaje(String chatId, String texto) {
        try {
            RespuestaTelegram respuesta = cliente.post()
                    .uri(URI.create(urlEnvio))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", chatId, "text", texto))
                    .retrieve()
                    .body(RespuestaTelegram.class);
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

    private String descripcionDe(RespuestaTelegram respuesta) {
        return respuesta == null || respuesta.description() == null
                ? SIN_MOTIVO.formatted(200)
                : respuesta.description();
    }

    private String descripcionDe(RestClientResponseException ex) {
        try {
            RespuestaTelegram cuerpo = ex.getResponseBodyAs(RespuestaTelegram.class);
            return cuerpo == null || cuerpo.description() == null
                    ? SIN_MOTIVO.formatted(ex.getStatusCode().value())
                    : cuerpo.description();
        } catch (RuntimeException noEsJson) {
            return SIN_MOTIVO.formatted(ex.getStatusCode().value());
        }
    }

    /** Respuesta de la Bot API. Solo se leen los dos campos que deciden el estado del envío. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaTelegram(boolean ok, String description) {
    }
}
