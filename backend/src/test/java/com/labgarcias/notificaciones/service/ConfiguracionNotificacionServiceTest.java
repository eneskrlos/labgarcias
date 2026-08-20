package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.ConfiguracionNotificacion;
import com.labgarcias.notificaciones.dto.ConfiguracionNotificacionRequest;
import com.labgarcias.notificaciones.dto.ConfiguracionNotificacionResponse;
import com.labgarcias.notificaciones.repository.ConfiguracionNotificacionRepository;
import com.labgarcias.shared.excepcion.ReglaNegocioException;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class ConfiguracionNotificacionServiceTest {

    private static final long USUARIO = 1L;

    @Mock
    private ConfiguracionNotificacionRepository configuracionRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ConfiguracionNotificacionService configuracionNotificacionService;

    private ConfiguracionNotificacionRequest request(boolean telegram, String chatId) {
        return new ConfiguracionNotificacionRequest(true, true, telegram, chatId);
    }

    private void sinConfiguracionGuardada() {
        when(configuracionRepository.findByUsuarioId(USUARIO)).thenReturn(Optional.empty());
    }

    private void devuelveLoQueGuarda() {
        when(configuracionRepository.saveAndFlush(any(ConfiguracionNotificacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    /** §6.4: sin fila guardada se contestan los canales por defecto de §6.3, no un 404. */
    @Test
    void quienNuncaConfiguroRecibeLosCanalesPorDefecto() {
        sinConfiguracionGuardada();

        ConfiguracionNotificacionResponse respuesta = configuracionNotificacionService.obtenerMia(USUARIO);

        assertThat(respuesta.canalAppActivo()).isTrue();
        assertThat(respuesta.canalCorreoActivo()).isTrue();
        assertThat(respuesta.canalTelegramActivo()).isTrue();
        assertThat(respuesta.canalWhatsappActivo()).isFalse();
        // Nula es la señal de "esto es el valor por defecto, no algo que hayas elegido".
        assertThat(respuesta.fechaActualizacion()).isNull();
    }

    /**
     * El conjunto por defecto que se informa y el que usa el selector al notificar tienen que ser
     * el mismo: si divergieran, la pantalla mostraría canales por los que no se está recibiendo.
     */
    @Test
    void loQueSeInformaPorDefectoEsLoMismoQueUsaElSelector() {
        assertThat(ConfiguracionNotificacion.porDefecto().canalesActivos())
                .isEqualTo(ConfiguracionNotificacion.CANALES_POR_DEFECTO);
    }

    /** §6 criterio 4 / CU-21. */
    @Test
    void criterio4ActivarTelegramSinChatIdEsRechazado() {
        assertThatThrownBy(() -> configuracionNotificacionService.guardarMia(USUARIO, request(true, null)))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> {
                    assertThat(((ReglaNegocioException) ex).getCodigo()).isEqualTo("TELEGRAM_SIN_DESTINO");
                    assertThat(((ReglaNegocioException) ex).getCampo()).isEqualTo("telegramChatId");
                });
        verify(configuracionRepository, never()).saveAndFlush(any());
    }

    /** Un chat en blanco es no tener destino: se rechaza igual que el nulo. */
    @Test
    void criterio4UnChatIdEnBlancoTampocoAlcanza() {
        assertThatThrownBy(() -> configuracionNotificacionService.guardarMia(USUARIO, request(true, "   ")))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void telegramApagadoNoExigeChatId() {
        sinConfiguracionGuardada();
        devuelveLoQueGuarda();

        configuracionNotificacionService.guardarMia(USUARIO, request(false, null));

        verify(configuracionRepository).saveAndFlush(any(ConfiguracionNotificacion.class));
    }

    @Test
    void laPrimeraVezCreaLaConfiguracionAsociadaAlUsuario() {
        sinConfiguracionGuardada();
        devuelveLoQueGuarda();

        configuracionNotificacionService.guardarMia(USUARIO, request(true, "123456789"));

        ArgumentCaptor<ConfiguracionNotificacion> captor = ArgumentCaptor.forClass(ConfiguracionNotificacion.class);
        verify(configuracionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTelegramChatId()).isEqualTo("123456789");
        assertThat(captor.getValue().canalesActivos())
                .containsExactlyInAnyOrder(Canal.APP, Canal.CORREO, Canal.TELEGRAM);
        verify(entityManager).getReference(any(), any());
    }

    /** El PUT reemplaza la configuración entera: lo que no viene, no queda. */
    @Test
    void elPutReemplazaLaConfiguracionExistenteEnLugarDeMezclarla() {
        ConfiguracionNotificacion existente = new ConfiguracionNotificacion();
        existente.setCanalAppActivo(true);
        existente.setCanalTelegramActivo(true);
        existente.setTelegramChatId("viejo-chat");
        when(configuracionRepository.findByUsuarioId(USUARIO)).thenReturn(Optional.of(existente));
        devuelveLoQueGuarda();

        configuracionNotificacionService.guardarMia(USUARIO, request(false, null));

        assertThat(existente.isCanalTelegramActivo()).isFalse();
        assertThat(existente.getTelegramChatId()).isNull();
        // Ya existía: no se le vuelve a asignar el usuario.
        verify(entityManager, never()).getReference(any(), any());
    }

    /** P-18: el request no tiene WhatsApp, así que guardar no puede encenderlo por accidente. */
    @Test
    void p18GuardarNoPuedeEncenderWhatsapp() {
        sinConfiguracionGuardada();
        devuelveLoQueGuarda();

        configuracionNotificacionService.guardarMia(USUARIO, request(true, "123456789"));

        ArgumentCaptor<ConfiguracionNotificacion> captor = ArgumentCaptor.forClass(ConfiguracionNotificacion.class);
        verify(configuracionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().isCanalWhatsappActivo()).isFalse();
        assertThat(captor.getValue().canalesActivos()).doesNotContain(Canal.WHATSAPP);
    }
}
