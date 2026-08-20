package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.ConfiguracionNotificacion;
import com.labgarcias.notificaciones.domain.TipoEvento;
import com.labgarcias.notificaciones.repository.ConfiguracionNotificacionRepository;

@ExtendWith(MockitoExtension.class)
class SelectorCanalesTest {

    private static final long DESTINATARIO = 3L;

    @Mock
    private ConfiguracionNotificacionRepository configuracionRepository;

    @InjectMocks
    private SelectorCanales selectorCanales;

    private void sinConfiguracion() {
        when(configuracionRepository.findByUsuarioId(DESTINATARIO)).thenReturn(Optional.empty());
    }

    /** El mock se arma antes del when(): construirlo dentro de thenReturn() interrumpe el stubbing. */
    private void configuracionCon(boolean app, boolean correo, boolean telegram, boolean whatsapp) {
        ConfiguracionNotificacion configuracion = new ConfiguracionNotificacion();
        configuracion.setCanalAppActivo(app);
        configuracion.setCanalCorreoActivo(correo);
        configuracion.setCanalTelegramActivo(telegram);
        configuracion.setCanalWhatsappActivo(whatsapp);
        when(configuracionRepository.findByUsuarioId(DESTINATARIO)).thenReturn(Optional.of(configuracion));
    }

    /** §6.3: "Sin configuración: app + correo + Telegram para todos". */
    @Test
    void sinConfiguracionRigeElConjuntoPorDefectoDeLaSpec() {
        sinConfiguracion();

        assertThat(selectorCanales.canalesDe(TipoEvento.CAMBIO_ESTADO, DESTINATARIO))
                .containsExactlyInAnyOrder(Canal.APP, Canal.CORREO, Canal.TELEGRAM);
    }

    /** P-18: WhatsApp es solo estructura; ninguna configuración por defecto lo enciende. */
    @Test
    void whatsappNuncaEntraPorDefecto() {
        sinConfiguracion();

        assertThat(selectorCanales.canalesDe(TipoEvento.CAMBIO_ESTADO, DESTINATARIO))
                .doesNotContain(Canal.WHATSAPP);
    }

    /** RN-19: lo que el destinatario apagó no se envía, aunque el evento lo liste. */
    @Test
    void rn19LaConfiguracionDelDestinatarioRecortaLosCanalesDelEvento() {
        configuracionCon(true, false, false, false);

        assertThat(selectorCanales.canalesDe(TipoEvento.CAMBIO_ESTADO, DESTINATARIO))
                .containsExactly(Canal.APP);
    }

    /**
     * §6.2: NUEVA_ORDEN figura como correo + Telegram. Encender APP en la configuración no lo
     * agrega: el evento manda sobre qué canales son pertinentes, la configuración solo recorta.
     */
    @Test
    void laConfiguracionNoPuedeAgregarUnCanalQueElEventoNoContempla() {
        configuracionCon(true, true, true, true);

        assertThat(selectorCanales.canalesDe(TipoEvento.NUEVA_ORDEN, DESTINATARIO))
                .containsExactlyInAnyOrder(Canal.CORREO, Canal.TELEGRAM);
    }

    /** §3.1.b: el alta de credenciales va solo por correo — todavía no vinculó Telegram. */
    @Test
    void credencialesCreadasSoloViajaPorCorreo() {
        configuracionCon(true, true, true, true);

        assertThat(selectorCanales.canalesDe(TipoEvento.CREDENCIALES_CREADAS, DESTINATARIO))
                .containsExactly(Canal.CORREO);
    }

    /** Apagar todo deja la notificación sin envíos, no rompe: la campana la sigue mostrando. */
    @Test
    void unDestinatarioQueApagoTodoNoGeneraNingunEnvio() {
        configuracionCon(false, false, false, false);

        assertThat(selectorCanales.canalesDe(TipoEvento.CAMBIO_ESTADO, DESTINATARIO)).isEmpty();
    }
}
