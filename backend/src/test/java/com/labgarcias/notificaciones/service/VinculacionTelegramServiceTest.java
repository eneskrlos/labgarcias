package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.labgarcias.notificaciones.domain.TelegramTokenVinculacion;
import com.labgarcias.notificaciones.repository.TelegramTokenVinculacionRepository;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.service.UsuarioService;
import com.labgarcias.shared.excepcion.ReglaNegocioException;

/** §6.5: emitir el enlace, vincular con el token que devuelve el bot, y desvincular. */
@ExtendWith(MockitoExtension.class)
class VinculacionTelegramServiceTest {

    private static final long USUARIO_ID = 7L;
    private static final String CHAT_ID = "987654321";
    private static final String BOT = "labgarcias_bot";

    @Mock
    private TelegramTokenVinculacionRepository tokenRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private ClienteTelegram clienteTelegram;

    private VinculacionTelegramService vinculacionTelegramService;

    @BeforeEach
    void prepararServicio() {
        vinculacionTelegramService =
                new VinculacionTelegramService(tokenRepository, usuarioService, clienteTelegram);
    }

    /**
     * El token nace en la base con `fecha_emision`; acá se simula esa columna. El id del usuario
     * va lenient porque los casos que no vinculan cortan antes de mirarlo.
     */
    private TelegramTokenVinculacion emitido(OffsetDateTime fechaEmision) {
        Usuario usuario = mock(Usuario.class);
        lenient().when(usuario.getId()).thenReturn(USUARIO_ID);
        TelegramTokenVinculacion token = new TelegramTokenVinculacion(usuario, "abc123");
        ReflectionTestUtils.setField(token, "fechaEmision", fechaEmision);
        return token;
    }

    private void botConfigurado() {
        when(clienteTelegram.vinculacionHabilitada()).thenReturn(true);
        when(clienteTelegram.getNombreBot()).thenReturn(BOT);
        when(usuarioService.obtenerPorId(USUARIO_ID)).thenReturn(mock(Usuario.class));
        when(tokenRepository.save(any(TelegramTokenVinculacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    /** §6.5 paso 2: el enlace profundo lleva el nombre del bot y el token recién emitido. */
    @Test
    void generaElEnlaceProfundoConElTokenQueGuarda() {
        botConfigurado();

        String enlace = vinculacionTelegramService.generarEnlace(USUARIO_ID).enlace();

        ArgumentCaptor<TelegramTokenVinculacion> captor =
                ArgumentCaptor.forClass(TelegramTokenVinculacion.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(enlace).isEqualTo("https://t.me/" + BOT + "?start=" + captor.getValue().getToken());
    }

    /** Un token adivinable dejaría vincular el Telegram propio a la cuenta de otro. */
    @Test
    void cadaTokenEsDistintoYNoTrivial() {
        botConfigurado();

        String primero = vinculacionTelegramService.generarEnlace(USUARIO_ID).enlace();
        String segundo = vinculacionTelegramService.generarEnlace(USUARIO_ID).enlace();

        assertThat(primero).isNotEqualTo(segundo);
        assertThat(primero.substring(primero.indexOf("start=") + 6)).hasSize(32);
    }

    /** §6.5: sin bot configurado (P-20), la vinculación se rechaza con un motivo claro. */
    @Test
    void sinBotConfiguradoNoEmiteNingunToken() {
        when(clienteTelegram.vinculacionHabilitada()).thenReturn(false);

        assertThatThrownBy(() -> vinculacionTelegramService.generarEnlace(USUARIO_ID))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo())
                        .isEqualTo("TELEGRAM_NO_CONFIGURADO"));

        verify(tokenRepository, never()).save(any());
    }

    /** §6.5 paso 4: token vigente → se guarda el chat, se sella el token y se contesta que sí. */
    @Test
    void unTokenVigenteVinculaLaCuentaYSeMarcaUsado() {
        TelegramTokenVinculacion token = emitido(OffsetDateTime.now());
        when(tokenRepository.findByToken("abc123")).thenReturn(Optional.of(token));

        assertThat(vinculacionTelegramService.vincular("abc123", CHAT_ID)).isTrue();

        verify(usuarioService).vincularTelegram(USUARIO_ID, CHAT_ID);
        assertThat(token.getFechaUso()).isNotNull();
    }

    /** §6.5 criterio 2: un token inexistente no vincula. */
    @Test
    void unTokenInexistenteNoVincula() {
        when(tokenRepository.findByToken("noExiste")).thenReturn(Optional.empty());

        assertThat(vinculacionTelegramService.vincular("noExiste", CHAT_ID)).isFalse();

        verify(usuarioService, never()).vincularTelegram(anyLong(), anyString());
    }

    /** §6.5 criterio 2: un token ya usado no vuelve a vincular, ni siquiera desde otro chat. */
    @Test
    void unTokenYaUsadoNoVuelveAVincular() {
        TelegramTokenVinculacion token = emitido(OffsetDateTime.now());
        token.marcarUsado(OffsetDateTime.now());
        when(tokenRepository.findByToken("abc123")).thenReturn(Optional.of(token));

        assertThat(vinculacionTelegramService.vincular("abc123", CHAT_ID)).isFalse();

        verify(usuarioService, never()).vincularTelegram(anyLong(), anyString());
    }

    /**
     * D-21: la vigencia es de 15 minutos sobre `fecha_emision`. Un token vencido se trata igual
     * que uno inexistente: el bot responde el mismo error.
     */
    @Test
    void unTokenVencidoNoVincula() {
        int minutos = TelegramTokenVinculacion.MINUTOS_VIGENCIA_TOKEN_TELEGRAM;
        TelegramTokenVinculacion token = emitido(OffsetDateTime.now().minusMinutes(minutos + 1L));
        when(tokenRepository.findByToken("abc123")).thenReturn(Optional.of(token));

        assertThat(vinculacionTelegramService.vincular("abc123", CHAT_ID)).isFalse();

        verify(usuarioService, never()).vincularTelegram(anyLong(), anyString());
        assertThat(token.getFechaUso()).isNull();
    }

    /** El límite se prueba de los dos lados: recién emitido vincula, pasado el plazo no. */
    @Test
    void dentroDelPlazoTodaviaVincula() {
        int minutos = TelegramTokenVinculacion.MINUTOS_VIGENCIA_TOKEN_TELEGRAM;
        TelegramTokenVinculacion token = emitido(OffsetDateTime.now().minusMinutes(minutos - 1L));
        when(tokenRepository.findByToken("abc123")).thenReturn(Optional.of(token));

        assertThat(vinculacionTelegramService.vincular("abc123", CHAT_ID)).isTrue();
    }

    /** §6.5 paso 5: desvincular es del usuario autenticado y lo resuelve el módulo dueño. */
    @Test
    void desvincularDelegaEnElModuloQueOwneaAlUsuario() {
        vinculacionTelegramService.desvincular(USUARIO_ID);

        verify(usuarioService).desvincularTelegram(USUARIO_ID);
    }
}
