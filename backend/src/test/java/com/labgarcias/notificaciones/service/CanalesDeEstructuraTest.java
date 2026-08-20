package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.Notificacion;

/**
 * Plan.md T-21 y P-18: Telegram y WhatsApp entran como estructura. Estos tests no prueban una
 * funcionalidad, fijan una decisión: que hoy **no** hay integración, y que la ausencia se ve como
 * un envío FALLIDO con motivo y no como un envío que se pierde en silencio.
 *
 * Cuando T-32 convierta a Telegram en canal real, el test de Telegram tiene que fallar. Eso es
 * lo que se busca: que nadie dé por integrado lo que no lo está.
 */
class CanalesDeEstructuraTest {

    private final CanalTelegram canalTelegram = new CanalTelegram();
    private final CanalWhatsApp canalWhatsApp = new CanalWhatsApp();

    @Test
    void cadaAdaptadorAtiendeSoloSuCanal() {
        assertThat(canalTelegram.soporta(Canal.TELEGRAM)).isTrue();
        assertThat(canalTelegram.soporta(Canal.WHATSAPP)).isFalse();
        assertThat(canalWhatsApp.soporta(Canal.WHATSAPP)).isTrue();
        assertThat(canalWhatsApp.soporta(Canal.TELEGRAM)).isFalse();
    }

    /** T-32: hasta que exista el bot (P-20), cada intento queda FALLIDO con el motivo a la vista. */
    @Test
    void telegramTodaviaNoEnviaYLoDiceConClaridad()  {
        assertThatThrownBy(() -> canalTelegram.enviar(new Notificacion()))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessageContaining("no configurado");
    }

    /** P-18: WhatsApp necesita un proveedor pago que no se contrató; D-21 lo reemplazó por Telegram. */
    @Test
    void whatsappTodaviaNoEnviaYLoDiceConClaridad() {
        assertThatThrownBy(() -> canalWhatsApp.enviar(new Notificacion()))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessageContaining("no configurado");
    }
}
