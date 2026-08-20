package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.EstadoEnvio;
import com.labgarcias.notificaciones.domain.Notificacion;
import com.labgarcias.notificaciones.domain.NotificacionEnvio;
import com.labgarcias.notificaciones.repository.NotificacionEnvioRepository;

class DespachadorNotificacionesTest {

    private static final int TAMANO_LOTE = 50;

    private final NotificacionEnvioRepository envioRepository = mock(NotificacionEnvioRepository.class);

    private NotificacionEnvio envioPendiente(Canal canal) {
        NotificacionEnvio envio = new NotificacionEnvio();
        envio.setNotificacion(new Notificacion());
        envio.setCanal(canal);
        envio.setEstadoEnvio(EstadoEnvio.PENDIENTE);
        return envio;
    }

    /** Arma el despachador con los adaptadores que le quiera dar cada test. */
    private DespachadorNotificaciones despachadorCon(NotificacionEnvio envio, CanalNotificacion... adaptadores) {
        when(envioRepository.findByEstadoEnvioOrderByIdAsc(EstadoEnvio.PENDIENTE, Limit.of(TAMANO_LOTE)))
                .thenReturn(List.of(envio));
        return new DespachadorNotificaciones(envioRepository, List.of(adaptadores), TAMANO_LOTE);
    }

    /** El mock se arma antes del when(): construirlo dentro de thenReturn() interrumpe el stubbing. */
    private CanalNotificacion adaptadorDe(Canal canal) {
        CanalNotificacion adaptador = mock(CanalNotificacion.class);
        when(adaptador.soporta(canal)).thenReturn(true);
        return adaptador;
    }

    @Test
    void unEnvioResueltoQuedaEnviadoYConFecha() {
        NotificacionEnvio envio = envioPendiente(Canal.APP);

        despachadorCon(envio, adaptadorDe(Canal.APP)).despachar();

        assertThat(envio.getEstadoEnvio()).isEqualTo(EstadoEnvio.ENVIADO);
        assertThat(envio.getFechaEnvio()).isNotNull();
        assertThat(envio.getDetalleError()).isNull();
    }

    /** §6 criterio 2: el correo que falla deja el motivo, no tumba nada. */
    @Test
    void criterio2UnCorreoQueFallaQuedaFallidoConSuMotivo() {
        NotificacionEnvio envio = envioPendiente(Canal.CORREO);
        CanalNotificacion correo = adaptadorDe(Canal.CORREO);
        doThrow(new EnvioNoRealizadoException("Connection refused")).when(correo).enviar(any(Notificacion.class));

        despachadorCon(envio, correo).despachar();

        assertThat(envio.getEstadoEnvio()).isEqualTo(EstadoEnvio.FALLIDO);
        assertThat(envio.getDetalleError()).isEqualTo("Connection refused");
    }

    /**
     * Un adaptador que revienta con algo inesperado no puede dejar el envío colgado en PENDIENTE
     * ni cortar el lote: se registra como fallo igual que los previstos.
     */
    @Test
    void unFalloInesperadoDelAdaptadorTambienQuedaRegistrado() {
        NotificacionEnvio envio = envioPendiente(Canal.CORREO);
        CanalNotificacion correo = adaptadorDe(Canal.CORREO);
        doThrow(new IllegalStateException("boom")).when(correo).enviar(any(Notificacion.class));

        despachadorCon(envio, correo).despachar();

        assertThat(envio.getEstadoEnvio()).isEqualTo(EstadoEnvio.FALLIDO);
        assertThat(envio.getDetalleError()).isEqualTo("boom");
    }

    /** Una excepción sin mensaje no puede dejar `detalle_error` vacío: no habría qué diagnosticar. */
    @Test
    void unFalloSinMensajeIgualDejaUnDetalleLegible() {
        NotificacionEnvio envio = envioPendiente(Canal.CORREO);
        CanalNotificacion correo = adaptadorDe(Canal.CORREO);
        doThrow(new IllegalStateException()).when(correo).enviar(any(Notificacion.class));

        despachadorCon(envio, correo).despachar();

        assertThat(envio.getDetalleError()).isNotBlank();
    }

    /** Sin adaptador para ese canal el envío queda FALLIDO, nunca en un bucle infinito de PENDIENTE. */
    @Test
    void unCanalSinAdaptadorQuedaFallido() {
        NotificacionEnvio envio = envioPendiente(Canal.WHATSAPP);

        despachadorCon(envio, adaptadorDe(Canal.APP)).despachar();

        assertThat(envio.getEstadoEnvio()).isEqualTo(EstadoEnvio.FALLIDO);
        assertThat(envio.getDetalleError()).contains("WHATSAPP");
    }

    /**
     * §6.3: el despachador solo toma PENDIENTE. Reintentar los FALLIDO sin política documentada
     * —sin tope ni espera— golpearía sin fin a un servidor caído.
     */
    @Test
    void soloTomaLosEnviosPendientes() {
        NotificacionEnvio envio = envioPendiente(Canal.APP);

        despachadorCon(envio, adaptadorDe(Canal.APP)).despachar();

        verify(envioRepository).findByEstadoEnvioOrderByIdAsc(EstadoEnvio.PENDIENTE, Limit.of(TAMANO_LOTE));
    }
}
