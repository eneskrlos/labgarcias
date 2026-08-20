package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.labgarcias.notificaciones.domain.Notificacion;
import com.labgarcias.notificaciones.domain.TipoEvento;
import com.labgarcias.notificaciones.dto.NotificacionResponse;
import com.labgarcias.notificaciones.repository.NotificacionRepository;
import com.labgarcias.shared.dto.PaginaResponse;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;
import com.labgarcias.shared.excepcion.ValidacionException;

@ExtendWith(MockitoExtension.class)
class BandejaNotificacionServiceTest {

    private static final long DUENO = 3L;
    private static final long INTRUSO = 99L;
    private static final long ID_NOTIFICACION = 12L;

    private static final Pageable PRIMERA_PAGINA = PageRequest.of(0, 10);

    @Mock
    private NotificacionRepository notificacionRepository;

    @InjectMocks
    private BandejaNotificacionService bandejaNotificacionService;

    private Notificacion notificacion(boolean leida) {
        Notificacion notificacion = new Notificacion();
        notificacion.setTipoEvento(TipoEvento.CAMBIO_ESTADO);
        notificacion.setMensaje("El trabajo del paciente Código 1000 pasó a la etapa de Listo.");
        notificacion.setOrdenId(7L);
        notificacion.setLeida(leida);
        return notificacion;
    }

    @Test
    void sinFiltroTraeTodasLasPropias() {
        when(notificacionRepository.findByDestinatarioId(DUENO, PRIMERA_PAGINA))
                .thenReturn(new PageImpl<>(List.of(notificacion(false), notificacion(true))));

        PaginaResponse<NotificacionResponse> pagina =
                bandejaNotificacionService.listarMias(DUENO, null, PRIMERA_PAGINA);

        assertThat(pagina.contenido()).hasSize(2);
        verify(notificacionRepository, never()).findByDestinatarioIdAndLeida(any(), any(Boolean.class), any());
    }

    /** §6.4: `?leidas=false` es lo que consume la campana al desplegarse. */
    @Test
    void conElFiltroLeidasConsultaSoloEseSubconjunto() {
        when(notificacionRepository.findByDestinatarioIdAndLeida(DUENO, false, PRIMERA_PAGINA))
                .thenReturn(new PageImpl<>(List.of(notificacion(false))));

        assertThat(bandejaNotificacionService.listarMias(DUENO, false, PRIMERA_PAGINA).contenido()).hasSize(1);
    }

    /**
     * §6 criterio 3: el id del destinatario viaja siempre a la consulta. Si alguien lo sacara,
     * este test seguiría pasando pero el de rutas y el de repositorio no: por eso además la
     * verificación está en la forma de las consultas, que no exponen ningún findById suelto.
     */
    @Test
    void criterio3ElListadoSiempreFiltraPorElDestinatarioDelToken() {
        when(notificacionRepository.findByDestinatarioId(eq(DUENO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        bandejaNotificacionService.listarMias(DUENO, null, PRIMERA_PAGINA);

        verify(notificacionRepository).findByDestinatarioId(eq(DUENO), any(Pageable.class));
    }

    @Test
    void unTamanoDePaginaInvalidoSeRechazaAntesDeConsultar() {
        assertThatThrownBy(() -> bandejaNotificacionService.listarMias(DUENO, null, PageRequest.of(0, 25)))
                .isInstanceOf(ValidacionException.class)
                .satisfies(ex -> assertThat(((ValidacionException) ex).getCodigo()).isEqualTo("TAMANO_PAGINA_INVALIDO"));
        verify(notificacionRepository, never()).findByDestinatarioId(any(), any());
    }

    @Test
    void elContadorCuentaSoloLasNoLeidasDelUsuario() {
        when(notificacionRepository.countByDestinatarioIdAndLeidaFalse(DUENO)).thenReturn(4L);

        assertThat(bandejaNotificacionService.contarNoLeidas(DUENO).noLeidas()).isEqualTo(4L);
    }

    @Test
    void marcarLeidaSellaLaFechaDeLectura() {
        Notificacion sinLeer = notificacion(false);
        when(notificacionRepository.findByIdAndDestinatarioId(ID_NOTIFICACION, DUENO))
                .thenReturn(Optional.of(sinLeer));

        NotificacionResponse respuesta = bandejaNotificacionService.marcarLeida(ID_NOTIFICACION, DUENO);

        assertThat(respuesta.leida()).isTrue();
        assertThat(sinLeer.getFechaLectura()).isNotNull();
    }

    /** Volver a marcarla no puede pisar la fecha: la primera lectura es la que vale. */
    @Test
    void marcarDosVecesNoMueveLaFechaOriginal() {
        Notificacion yaLeida = notificacion(true);
        OffsetDateTime original = OffsetDateTime.now().minusDays(2);
        yaLeida.setFechaLectura(original);
        when(notificacionRepository.findByIdAndDestinatarioId(ID_NOTIFICACION, DUENO))
                .thenReturn(Optional.of(yaLeida));

        bandejaNotificacionService.marcarLeida(ID_NOTIFICACION, DUENO);

        assertThat(yaLeida.getFechaLectura()).isEqualTo(original);
    }

    /**
     * §6 criterio 3: una notificación ajena y una inexistente dan la misma respuesta. Un 403
     * delataría que el id existe (mismo criterio que RN-01 en las órdenes).
     */
    @Test
    void criterio3UnaNotificacionAjenaRespondeNoEncontrada() {
        when(notificacionRepository.findByIdAndDestinatarioId(ID_NOTIFICACION, INTRUSO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bandejaNotificacionService.marcarLeida(ID_NOTIFICACION, INTRUSO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getCodigo())
                        .isEqualTo("NOTIFICACION_NO_ENCONTRADA"));
    }

    @Test
    void leerTodasDevuelveElContadorYaEnCero() {
        when(notificacionRepository.countByDestinatarioIdAndLeidaFalse(DUENO)).thenReturn(0L);

        assertThat(bandejaNotificacionService.marcarTodasLeidas(DUENO).noLeidas()).isZero();
        verify(notificacionRepository).marcarTodasLeidas(eq(DUENO), any(OffsetDateTime.class));
    }
}
