package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.Notificacion;
import com.labgarcias.seguridad.domain.Usuario;

/**
 * P-18: WhatsApp entra como estructura. Este test no prueba una funcionalidad, fija una decisión:
 * que hoy **no** hay integración, y que la ausencia se ve como un envío FALLIDO con motivo y no
 * como un envío que se pierde en silencio.
 *
 * T-32 dio vuelta la mitad de este archivo: Telegram dejó de ser estructura y sus tests se mudaron
 * a `CanalTelegramTest`. Lo que quedó acá es lo que sigue sin integrarse, y sigue vigente el mismo
 * criterio: el día que exista un proveedor de WhatsApp, este test tiene que fallar. Eso es lo que
 * se busca, que nadie dé por integrado lo que no lo está.
 */
class CanalesDeEstructuraTest {

    private final CanalWhatsApp canalWhatsApp = new CanalWhatsApp();
    private final CanalTelegram canalTelegram =
            new CanalTelegram(new ClienteTelegram(RestClient.builder(), "", ""));

    private Notificacion notificacionCon(String telefono) {
        Usuario destinatario = new Usuario();
        destinatario.setTelefono(telefono);
        Notificacion notificacion = new Notificacion();
        notificacion.setDestinatario(destinatario);
        notificacion.setMensaje("Aviso de prueba.");
        return notificacion;
    }

    /** Cada adaptador atiende un solo canal: dos adaptadores no pueden pelearse un mismo envío. */
    @Test
    void cadaAdaptadorAtiendeSoloSuCanal() {
        assertThat(canalWhatsApp.soporta(Canal.WHATSAPP)).isTrue();
        assertThat(canalWhatsApp.soporta(Canal.TELEGRAM)).isFalse();
        assertThat(canalTelegram.soporta(Canal.TELEGRAM)).isTrue();
        assertThat(canalTelegram.soporta(Canal.WHATSAPP)).isFalse();
    }

    /** P-18: WhatsApp necesita un proveedor pago que no se contrató; D-21 lo reemplazó por Telegram. */
    @Test
    void whatsappTodaviaNoEnviaYLoDiceConClaridad() {
        assertThatThrownBy(() -> canalWhatsApp.enviar(notificacionCon("+59891234567")))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessageContaining("no configurado");
    }

    /**
     * §6.3 pide validar el teléfono aunque el canal no exista todavía: "sin destino" y "sin
     * proveedor" son dos problemas distintos y se tienen que poder distinguir en `detalle_error`.
     */
    @Test
    void whatsappDistingueLaFaltaDeTelefonoDeLaFaltaDeProveedor() {
        assertThatThrownBy(() -> canalWhatsApp.enviar(notificacionCon(null)))
                .isInstanceOf(EnvioNoRealizadoException.class)
                .hasMessageContaining("sin destino");
    }
}
