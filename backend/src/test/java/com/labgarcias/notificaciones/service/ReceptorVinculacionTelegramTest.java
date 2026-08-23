package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.labgarcias.notificaciones.service.ClienteTelegram.MensajeRecibido;

/**
 * §6.5 paso 4: lo que hace el sistema con lo que el bot recibe. El polling en sí es de Spring;
 * lo que se prueba acá es qué se vincula, qué se contesta y cómo avanza el offset.
 */
class ReceptorVinculacionTelegramTest {

    private static final String CHAT_ID = "987654321";
    private static final String TEXTO_EXITO = "✅ Cuenta vinculada.";

    private final ClienteTelegram clienteTelegram = mock(ClienteTelegram.class);
    private final VinculacionTelegramService vinculacionTelegramService =
            mock(VinculacionTelegramService.class);
    private final ReceptorVinculacionTelegram receptor =
            new ReceptorVinculacionTelegram(clienteTelegram, vinculacionTelegramService);

    private void llegan(MensajeRecibido... mensajes) {
        when(clienteTelegram.vinculacionHabilitada()).thenReturn(true);
        when(clienteTelegram.obtenerActualizaciones(anyLong())).thenReturn(List.of(mensajes));
    }

    private String respuestaDelBot() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(clienteTelegram).enviarMensaje(anyString(), captor.capture());
        return captor.getValue();
    }

    /** §6.5: sin bot configurado el proceso programado no puede golpear a la API cada 5 segundos. */
    @Test
    void sinBotConfiguradoNoConsultaNada() {
        when(clienteTelegram.vinculacionHabilitada()).thenReturn(false);

        receptor.recibir();

        verify(clienteTelegram, never()).obtenerActualizaciones(anyLong());
    }

    /** §6.5 paso 4: el `/start {token}` vincula y el bot confirma con el texto documentado. */
    @Test
    void unStartConTokenValidoVinculaYConfirmaPorElBot() {
        llegan(new MensajeRecibido(10, CHAT_ID, "/start abc123"));
        when(vinculacionTelegramService.vincular("abc123", CHAT_ID)).thenReturn(true);

        receptor.recibir();

        verify(vinculacionTelegramService).vincular("abc123", CHAT_ID);
        assertThat(respuestaDelBot()).startsWith(TEXTO_EXITO);
    }

    /** §6.5 criterio 2: un token que no vincula tiene que decírselo a quien escribió. */
    @Test
    void criterio2UnTokenQueNoVinculaRecibeElErrorPorElBot() {
        llegan(new MensajeRecibido(11, CHAT_ID, "/start vencido"));
        when(vinculacionTelegramService.vincular("vencido", CHAT_ID)).thenReturn(false);

        receptor.recibir();

        assertThat(respuestaDelBot())
                .doesNotStartWith(TEXTO_EXITO)
                .contains("No pudimos vincular");
    }

    /** §6.5 solo define `/start {token}`: al resto no se le inventa conversación. */
    @Test
    void loQueNoEsUnStartConTokenSeIgnoraEnSilencio() {
        llegan(new MensajeRecibido(12, CHAT_ID, "hola"),
                new MensajeRecibido(13, CHAT_ID, "/start"),
                new MensajeRecibido(14, CHAT_ID, "/start   "),
                new MensajeRecibido(15, null, null));

        receptor.recibir();

        verify(vinculacionTelegramService, never()).vincular(anyString(), anyString());
        verify(clienteTelegram, never()).enviarMensaje(anyString(), anyString());
    }

    /**
     * El offset arranca en 0 y avanza al id siguiente al último visto —incluso si esa novedad no
     * era un mensaje—. Es lo que confirma las novedades contra Telegram: sin esto, las mismas
     * volverían en cada corrida.
     */
    @Test
    void elOffsetAvanzaAlSiguienteDeLaUltimaNovedadVista() {
        llegan(new MensajeRecibido(30, null, null), new MensajeRecibido(31, CHAT_ID, "hola"));

        receptor.recibir();
        receptor.recibir();

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(clienteTelegram, times(2)).obtenerActualizaciones(captor.capture());
        assertThat(captor.getAllValues()).containsExactly(0L, 32L);
    }

    /** Un mensaje que falla no puede cortar la tanda: el offset ya avanzó y no se reintenta. */
    @Test
    void unFalloAtendiendoUnMensajeNoCortaLaTanda() {
        llegan(new MensajeRecibido(40, CHAT_ID, "/start uno"),
                new MensajeRecibido(41, CHAT_ID, "/start dos"));
        doThrow(new EnvioNoRealizadoException("Telegram rechazó el envío: chat not found"))
                .when(clienteTelegram).enviarMensaje(anyString(), anyString());

        receptor.recibir();

        verify(vinculacionTelegramService).vincular("uno", CHAT_ID);
        verify(vinculacionTelegramService).vincular("dos", CHAT_ID);
    }
}
