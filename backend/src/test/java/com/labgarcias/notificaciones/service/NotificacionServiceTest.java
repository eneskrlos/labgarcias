package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.EstadoEnvio;
import com.labgarcias.notificaciones.domain.Notificacion;
import com.labgarcias.notificaciones.domain.NotificacionEnvio;
import com.labgarcias.notificaciones.domain.TipoEvento;
import com.labgarcias.notificaciones.repository.NotificacionEnvioRepository;
import com.labgarcias.notificaciones.repository.NotificacionRepository;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    private static final long DESTINATARIO = 3L;
    private static final long ORDEN = 12L;
    private static final String MENSAJE = "El trabajo del paciente Código 1000 pasó a la etapa de Listo.";

    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private NotificacionEnvioRepository envioRepository;
    @Mock
    private SelectorCanales selectorCanales;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private NotificacionService notificacionService;

    @Captor
    private ArgumentCaptor<List<NotificacionEnvio>> enviosCaptor;

    @BeforeEach
    void devolverLaNotificacionGuardada() {
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void canalesActivos(Canal... canales) {
        when(selectorCanales.canalesDe(TipoEvento.CAMBIO_ESTADO, DESTINATARIO)).thenReturn(Set.of(canales));
    }

    private Notificacion registrar() {
        return notificacionService.registrar(DESTINATARIO, TipoEvento.CAMBIO_ESTADO, MENSAJE, ORDEN);
    }

    @Test
    void guardaLaNotificacionConSuEventoMensajeYOrden() {
        canalesActivos(Canal.APP);

        Notificacion notificacion = registrar();

        assertThat(notificacion.getTipoEvento()).isEqualTo(TipoEvento.CAMBIO_ESTADO);
        assertThat(notificacion.getMensaje()).isEqualTo(MENSAJE);
        assertThat(notificacion.getOrdenId()).isEqualTo(ORDEN);
        assertThat(notificacion.isLeida()).isFalse();
    }

    /** §6 criterio 1: un envío por cada canal activo, todos PENDIENTE hasta que el despachador actúe. */
    @Test
    void criterio1CreaUnEnvioPendientePorCadaCanalActivo() {
        canalesActivos(Canal.APP, Canal.CORREO, Canal.TELEGRAM);

        registrar();

        verify(envioRepository).saveAll(enviosCaptor.capture());
        assertThat(enviosCaptor.getValue())
                .hasSize(3)
                .allSatisfy(envio -> assertThat(envio.getEstadoEnvio()).isEqualTo(EstadoEnvio.PENDIENTE))
                .extracting(NotificacionEnvio::getCanal)
                .containsExactlyInAnyOrder(Canal.APP, Canal.CORREO, Canal.TELEGRAM);
    }

    /** RN-19: los canales no los decide este servicio, los decide el selector. */
    @Test
    void rn19NoInventaCanalesPorSuCuenta() {
        canalesActivos(Canal.CORREO);

        registrar();

        verify(envioRepository).saveAll(enviosCaptor.capture());
        assertThat(enviosCaptor.getValue()).extracting(NotificacionEnvio::getCanal).containsExactly(Canal.CORREO);
    }

    /**
     * §6 criterio 2 en su forma extrema: sin ningún canal activo la notificación se guarda igual,
     * porque la campana de §6.4 lee `notificacion`, no `notificacion_envio`.
     */
    @Test
    void sinCanalesActivosLaNotificacionSeGuardaIgual() {
        canalesActivos();

        registrar();

        verify(notificacionRepository).save(any(Notificacion.class));
        verify(envioRepository).saveAll(enviosCaptor.capture());
        assertThat(enviosCaptor.getValue()).isEmpty();
    }
}
