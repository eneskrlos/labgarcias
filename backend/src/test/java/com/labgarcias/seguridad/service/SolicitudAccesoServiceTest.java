package com.labgarcias.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.labgarcias.seguridad.domain.EstadoSolicitud;
import com.labgarcias.seguridad.domain.SolicitudAcceso;
import com.labgarcias.seguridad.domain.SolicitudAccesoEvent;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.dto.SolicitudAccesoRequest;
import com.labgarcias.seguridad.dto.SolicitudAccesoResponse;
import com.labgarcias.seguridad.repository.SolicitudAccesoRepository;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.dto.PaginaResponse;
import com.labgarcias.shared.excepcion.ConflictoException;
import com.labgarcias.shared.excepcion.DominioException;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;
import com.labgarcias.shared.excepcion.ValidacionException;

@ExtendWith(MockitoExtension.class)
class SolicitudAccesoServiceTest {

    private static final String CORREO = "juan@mail.com";

    @Mock
    private SolicitudAccesoRepository solicitudAccesoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ApplicationEventPublisher publicadorEventos;

    @InjectMocks
    private SolicitudAccesoService solicitudAccesoService;

    private SolicitudAccesoRequest request;

    @BeforeEach
    void prepararRequest() {
        request = new SolicitudAccesoRequest("Dr. Juan Pérez", CORREO, "Av. 18 de Julio 1234", "+59891234567");
    }

    private void correoLibre() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());
        when(solicitudAccesoRepository.existsByCorreoIgnoreCaseAndEstado(CORREO, EstadoSolicitud.PENDIENTE))
                .thenReturn(false);
        when(solicitudAccesoRepository.save(any(SolicitudAcceso.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private SolicitudAcceso solicitudEn(EstadoSolicitud estado) {
        SolicitudAcceso solicitud = new SolicitudAcceso();
        solicitud.setNombreCompleto("Dr. Juan Pérez");
        solicitud.setCorreo(CORREO);
        solicitud.setDireccion("Av. 18 de Julio 1234");
        solicitud.setTelefono("+59891234567");
        solicitud.setEstado(estado);
        return solicitud;
    }

    /** §3.1: la solicitud nace PENDIENTE con los datos del formulario. */
    @Test
    void registrarGuardaLaSolicitudComoPendiente() {
        correoLibre();

        solicitudAccesoService.registrar(request);

        ArgumentCaptor<SolicitudAcceso> capturada = ArgumentCaptor.forClass(SolicitudAcceso.class);
        verify(solicitudAccesoRepository).save(capturada.capture());
        assertThat(capturada.getValue().getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(capturada.getValue().getCorreo()).isEqualTo(CORREO);
        assertThat(capturada.getValue().getNombreCompleto()).isEqualTo("Dr. Juan Pérez");
        assertThat(capturada.getValue().getFechaResolucion()).isNull();
    }

    /** §3.1 criterio 1: la solicitud no crea usuario ni permite login. */
    @Test
    void criterio1RegistrarNoCreaNingunUsuario() {
        correoLibre();

        solicitudAccesoService.registrar(request);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    /** §3.1: publica SolicitudAccesoEvent, que es lo que dispara el aviso al administrador. */
    @Test
    void registrarPublicaElEventoParaNotificarAlAdministrador() {
        correoLibre();

        solicitudAccesoService.registrar(request);

        ArgumentCaptor<SolicitudAccesoEvent> evento = ArgumentCaptor.forClass(SolicitudAccesoEvent.class);
        verify(publicadorEventos).publishEvent(evento.capture());
        assertThat(evento.getValue().nombreCompleto()).isEqualTo("Dr. Juan Pérez");
    }

    /** §3.1: si ya existe un usuario con ese correo → 409 CORREO_YA_REGISTRADO. */
    @Test
    void registrarRechazaUnCorreoQueYaTieneCuenta() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> solicitudAccesoService.registrar(request))
                .isInstanceOf(ConflictoException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("CORREO_YA_REGISTRADO");

        verify(solicitudAccesoRepository, never()).save(any(SolicitudAcceso.class));
        verify(publicadorEventos, never()).publishEvent(any(SolicitudAccesoEvent.class));
    }

    /** §3.1 criterio 3: un correo con solicitud pendiente no puede duplicarla. */
    @Test
    void criterio3RegistrarRechazaUnaSegundaSolicitudPendiente() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());
        when(solicitudAccesoRepository.existsByCorreoIgnoreCaseAndEstado(CORREO, EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThatThrownBy(() -> solicitudAccesoService.registrar(request))
                .isInstanceOf(ConflictoException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("SOLICITUD_YA_EXISTENTE");

        verify(solicitudAccesoRepository, never()).save(any(SolicitudAcceso.class));
    }

    /** §3.1.b: rechazar deja la solicitud RECHAZADA y sella su fecha de resolución. */
    @Test
    void rechazarMarcaLaSolicitudYSellaLaFecha() {
        SolicitudAcceso pendiente = solicitudEn(EstadoSolicitud.PENDIENTE);
        when(solicitudAccesoRepository.findById(7L)).thenReturn(Optional.of(pendiente));

        SolicitudAccesoResponse respuesta = solicitudAccesoService.rechazar(7L);

        assertThat(pendiente.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(pendiente.getFechaResolucion()).isNotNull();
        assertThat(respuesta.estado()).isEqualTo("RECHAZADA");
    }

    @Test
    void rechazarUnaSolicitudInexistenteResponde404() {
        when(solicitudAccesoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> solicitudAccesoService.rechazar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("SOLICITUD_NO_ENCONTRADA");
    }

    /** Una solicitud ya aprobada no se puede rechazar: sería revertir un alta que ya ocurrió. */
    @Test
    void rechazarUnaSolicitudYaResueltaResponde409() {
        when(solicitudAccesoRepository.findById(7L)).thenReturn(Optional.of(solicitudEn(EstadoSolicitud.APROBADA)));

        assertThatThrownBy(() -> solicitudAccesoService.rechazar(7L))
                .isInstanceOf(ConflictoException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("SOLICITUD_YA_RESUELTA");
    }

    /** §3.1.b criterio 4: el alta de la cuenta es lo que aprueba la solicitud. */
    @Test
    void criterio4AprobarMarcaLaSolicitudYSellaLaFecha() {
        SolicitudAcceso pendiente = solicitudEn(EstadoSolicitud.PENDIENTE);
        when(solicitudAccesoRepository.findById(12L)).thenReturn(Optional.of(pendiente));

        solicitudAccesoService.aprobar(12L);

        assertThat(pendiente.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
        assertThat(pendiente.getFechaResolucion()).isNotNull();
    }

    @Test
    void aprobarUnaSolicitudInexistenteResponde404() {
        when(solicitudAccesoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> solicitudAccesoService.aprobar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("SOLICITUD_NO_ENCONTRADA");
    }

    /** Una solicitud ya rechazada no puede aprobarse por la puerta de atrás del alta. */
    @Test
    void aprobarUnaSolicitudYaResueltaResponde409() {
        when(solicitudAccesoRepository.findById(12L))
                .thenReturn(Optional.of(solicitudEn(EstadoSolicitud.RECHAZADA)));

        assertThatThrownBy(() -> solicitudAccesoService.aprobar(12L))
                .isInstanceOf(ConflictoException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("SOLICITUD_YA_RESUELTA");
    }

    /** §8.1 Regla 2: el tamaño de página lo valida el backend. */
    @Test
    void listarRechazaUnTamanoDePaginaNoPermitido() {
        assertThatThrownBy(() -> solicitudAccesoService.listarPaginado(PageRequest.of(0, 25), null))
                .isInstanceOf(ValidacionException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("TAMANO_PAGINA_INVALIDO");
    }

    @Test
    void listarSinFiltroTraeTodasLasSolicitudes() {
        Pageable pageable = PageRequest.of(0, 10);
        when(solicitudAccesoRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(solicitudEn(EstadoSolicitud.RECHAZADA)), pageable, 1));

        PaginaResponse<SolicitudAccesoResponse> pagina = solicitudAccesoService.listarPaginado(pageable, null);

        assertThat(pagina.contenido()).hasSize(1);
        verify(solicitudAccesoRepository, never()).findByEstado(any(EstadoSolicitud.class), any(Pageable.class));
    }

    /** §3.1.b documenta el filtro con ?estado=PENDIENTE: es lo que consume la pantalla del admin. */
    @Test
    void listarConFiltroConsultaSoloEseEstado() {
        Pageable pageable = PageRequest.of(0, 10);
        when(solicitudAccesoRepository.findByEstado(EstadoSolicitud.PENDIENTE, pageable))
                .thenReturn(new PageImpl<>(List.of(solicitudEn(EstadoSolicitud.PENDIENTE)), pageable, 1));

        PaginaResponse<SolicitudAccesoResponse> pagina = solicitudAccesoService.listarPaginado(pageable, "pendiente");

        assertThat(pagina.contenido()).singleElement()
                .extracting(SolicitudAccesoResponse::estado).isEqualTo("PENDIENTE");
    }

    /** Un estado inventado en la URL da 400 con su código, no un 500 del manejador genérico. */
    @Test
    void listarRechazaUnEstadoInexistente() {
        assertThatThrownBy(() -> solicitudAccesoService.listarPaginado(PageRequest.of(0, 10), "ARCHIVADA"))
                .isInstanceOf(ValidacionException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("ESTADO_SOLICITUD_INVALIDO");

        verify(solicitudAccesoRepository, never()).findByEstado(any(EstadoSolicitud.class), any(Pageable.class));
        verify(solicitudAccesoRepository, never()).findAll(any(Pageable.class));
    }

    /** El correo se compara sin distinguir mayúsculas: es la misma regla del índice de usuario. */
    @Test
    void registrarComparaElCorreoSinDistinguirMayusculas() {
        correoLibre();

        solicitudAccesoService.registrar(request);

        verify(usuarioRepository).findByCorreoIgnoreCase(CORREO);
        verify(solicitudAccesoRepository).existsByCorreoIgnoreCaseAndEstado(anyString(), eq(EstadoSolicitud.PENDIENTE));
    }
}
