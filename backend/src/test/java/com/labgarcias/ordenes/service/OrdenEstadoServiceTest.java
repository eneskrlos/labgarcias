package com.labgarcias.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.labgarcias.catalogos.domain.Estado;
import com.labgarcias.catalogos.service.EstadoService;
import com.labgarcias.ordenes.domain.EstadoOrdenCambiadoEvent;
import com.labgarcias.ordenes.domain.Orden;
import com.labgarcias.ordenes.domain.OrdenHistorialEstado;
import com.labgarcias.ordenes.repository.OrdenHistorialEstadoRepository;
import com.labgarcias.ordenes.repository.OrdenRepository;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.shared.excepcion.ConflictoException;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class OrdenEstadoServiceTest {

    private static final long ID_ORDEN = 1L;
    private static final long ID_DUENO = 7L;
    private static final long ID_INTRUSO = 99L;
    private static final long ID_ADMIN = 3L;

    @Mock
    private OrdenRepository ordenRepository;
    @Mock
    private OrdenHistorialEstadoRepository historialRepository;
    @Mock
    private EstadoService estadoService;
    @Mock
    private OrdenService ordenService;
    @Mock
    private ApplicationEventPublisher eventos;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private OrdenEstadoService ordenEstadoService;

    /** Los siete estados de spec.md §4.2: catálogo cerrado, secuencia y terminalidad fijas. */
    private Estado recibido;
    private Estado enEvaluacion;
    private Estado enProduccion;
    private Estado controlCalidad;
    private Estado listo;
    private Estado entregado;
    private Estado cancelado;

    private static Estado estado(String codigo, String nombre, Integer secuencia, boolean terminal) {
        Estado estado = new Estado();
        estado.setCodigo(codigo);
        estado.setNombre(nombre);
        estado.setOrdenSecuencia(secuencia == null ? null : secuencia.shortValue());
        estado.setEsTerminal(terminal);
        estado.setEsProductivo(secuencia != null);
        return estado;
    }

    @BeforeEach
    void prepararCatalogoDeEstados() {
        recibido = estado("RECIBIDO", "Recibido", 1, false);
        enEvaluacion = estado("EN_EVALUACION", "En evaluacion", 2, false);
        enProduccion = estado("EN_PRODUCCION", "En produccion", 3, false);
        controlCalidad = estado("CONTROL_CALIDAD", "Control de calidad", 4, false);
        listo = estado("LISTO", "Listo", 5, false);
        entregado = estado("ENTREGADO", "Entregado", 6, true);
        // RN-04: CANCELADO vive fuera del flujo productivo, por eso su secuencia es null.
        cancelado = estado("CANCELADO", "Cancelado", null, true);
    }

    private Orden ordenEn(Estado estadoActual) {
        Usuario dueno = mock(Usuario.class);
        lenient().when(dueno.getId()).thenReturn(ID_DUENO);
        Orden orden = mock(Orden.class);
        lenient().when(orden.getId()).thenReturn(ID_ORDEN);
        lenient().when(orden.getCodigo()).thenReturn("LG-0001");
        lenient().when(orden.getPacienteCodigo()).thenReturn(1000);
        lenient().when(orden.getOdontologo()).thenReturn(dueno);
        lenient().when(orden.getEstado()).thenReturn(estadoActual);
        return orden;
    }

    private Orden prepararOrdenEn(Estado estadoActual) {
        Orden orden = ordenEn(estadoActual);
        when(ordenRepository.findById(ID_ORDEN)).thenReturn(Optional.of(orden));
        return orden;
    }

    private void catalogoDevuelve(Estado destino) {
        when(estadoService.obtenerPorCodigoParaOrden(destino.getCodigo())).thenReturn(destino);
    }

    // --- CU-06 / §5.5: avance de estado ---

    @Test
    void avanzarAlEstadoInmediatamenteSiguienteActualizaLaOrden() {
        Orden orden = prepararOrdenEn(recibido);
        catalogoDevuelve(enEvaluacion);

        ordenEstadoService.avanzarEstado(ID_ORDEN, "EN_EVALUACION", ID_ADMIN);

        verify(orden).setEstado(enEvaluacion);
        verify(ordenService).obtenerDetalle(ID_ORDEN, ID_ADMIN, true);
    }

    /** §5.5 criterio 1. */
    @Test
    void criterio1SaltarDeRecibidoAListoEsRechazado() {
        prepararOrdenEn(recibido);
        catalogoDevuelve(listo);

        assertThatThrownBy(() -> ordenEstadoService.avanzarEstado(ID_ORDEN, "LISTO", ID_ADMIN))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigo()).isEqualTo("TRANSICION_NO_PERMITIDA"))
                .satisfies(ex -> assertThat(((ConflictoException) ex).getHttpStatus().value()).isEqualTo(409));

        verify(historialRepository, never()).saveAndFlush(any());
    }

    /** §5.5 criterio 2: P-02 sin resolver, no hay marcha atrás. */
    @Test
    void criterio2RetrocederDeControlCalidadAEnProduccionEsRechazado() {
        prepararOrdenEn(controlCalidad);
        catalogoDevuelve(enProduccion);

        assertThatThrownBy(() -> ordenEstadoService.avanzarEstado(ID_ORDEN, "EN_PRODUCCION", ID_ADMIN))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigo()).isEqualTo("TRANSICION_NO_PERMITIDA"));
    }

    /** §5.5 criterio 3: la transición queda registrada, y la fecha la sella la base. */
    @Test
    void criterio3CadaTransicionDejaSuRegistroEnElHistorialConSuAutor() {
        Orden orden = prepararOrdenEn(enProduccion);
        catalogoDevuelve(controlCalidad);
        Usuario autor = mock(Usuario.class);
        when(entityManager.getReference(Usuario.class, ID_ADMIN)).thenReturn(autor);

        ordenEstadoService.avanzarEstado(ID_ORDEN, "CONTROL_CALIDAD", ID_ADMIN);

        ArgumentCaptor<OrdenHistorialEstado> captor = ArgumentCaptor.forClass(OrdenHistorialEstado.class);
        verify(historialRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getOrden()).isSameAs(orden);
        assertThat(captor.getValue().getEstado()).isSameAs(controlCalidad);
        assertThat(captor.getValue().getUsuario()).isSameAs(autor);
    }

    /** §5.5 criterio 4: una transición rechazada no publica evento, así que no hay qué notificar. */
    @Test
    void criterio4UnaTransicionRechazadaNoPublicaEvento() {
        prepararOrdenEn(recibido);
        catalogoDevuelve(entregado);

        assertThatThrownBy(() -> ordenEstadoService.avanzarEstado(ID_ORDEN, "ENTREGADO", ID_ADMIN))
                .isInstanceOf(ConflictoException.class);

        verify(eventos, never()).publishEvent(any(EstadoOrdenCambiadoEvent.class));
    }

    /** §6.2: el texto de CU-07 se arma con el código de paciente y el nombre del estado. */
    @Test
    void publicaElEventoConLosDatosQueNecesitaLaNotificacion() {
        prepararOrdenEn(enEvaluacion);
        catalogoDevuelve(enProduccion);

        ordenEstadoService.avanzarEstado(ID_ORDEN, "EN_PRODUCCION", ID_ADMIN);

        ArgumentCaptor<EstadoOrdenCambiadoEvent> captor = ArgumentCaptor.forClass(EstadoOrdenCambiadoEvent.class);
        verify(eventos).publishEvent(captor.capture());
        EstadoOrdenCambiadoEvent evento = captor.getValue();
        assertThat(evento.ordenId()).isEqualTo(ID_ORDEN);
        assertThat(evento.codigo()).isEqualTo("LG-0001");
        assertThat(evento.odontologoId()).isEqualTo(ID_DUENO);
        assertThat(evento.pacienteCodigo()).isEqualTo(1000);
        assertThat(evento.estadoNombre()).isEqualTo("En produccion");
    }

    @Test
    void desdeUnEstadoTerminalNoHayTransicion() {
        prepararOrdenEn(entregado);

        assertThatThrownBy(() -> ordenEstadoService.avanzarEstado(ID_ORDEN, "CANCELADO", ID_ADMIN))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigo()).isEqualTo("TRANSICION_NO_PERMITIDA"));
    }

    /** RN-17/CU-20: cancelar es del odontólogo; el laboratorio no llega a CANCELADO por acá. */
    @Test
    void elAdministradorNoPuedeCancelarPorElEndpointDeEstado() {
        prepararOrdenEn(enProduccion);
        catalogoDevuelve(cancelado);

        assertThatThrownBy(() -> ordenEstadoService.avanzarEstado(ID_ORDEN, "CANCELADO", ID_ADMIN))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigo()).isEqualTo("TRANSICION_NO_PERMITIDA"));

        verify(historialRepository, never()).saveAndFlush(any());
        verify(eventos, never()).publishEvent(any(EstadoOrdenCambiadoEvent.class));
    }

    @Test
    void avanzarUnaOrdenInexistenteDevuelve404() {
        when(ordenRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenEstadoService.avanzarEstado(404L, "EN_EVALUACION", ID_ADMIN))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // --- CU-20 / §5.6: cancelación ---

    @Test
    void elDuenoCancelaSuOrdenYQuedaRegistrada() {
        Orden orden = prepararOrdenEn(enProduccion);
        catalogoDevuelve(cancelado);

        ordenEstadoService.cancelar(ID_ORDEN, ID_DUENO);

        verify(orden).setEstado(cancelado);
        verify(orden).setFechaCancelacion(any(OffsetDateTime.class));
        verify(historialRepository).saveAndFlush(any(OrdenHistorialEstado.class));
    }

    /** §5.6 criterio 2. */
    @Test
    void criterio2UnaOrdenEntregadaNoPuedeCancelarse() {
        prepararOrdenEn(entregado);

        assertThatThrownBy(() -> ordenEstadoService.cancelar(ID_ORDEN, ID_DUENO))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigo()).isEqualTo("ORDEN_NO_CANCELABLE"))
                .satisfies(ex -> assertThat(((ConflictoException) ex).getHttpStatus().value()).isEqualTo(409));

        verify(historialRepository, never()).saveAndFlush(any());
    }

    @Test
    void unaOrdenYaCanceladaNoPuedeVolverACancelarse() {
        prepararOrdenEn(cancelado);

        assertThatThrownBy(() -> ordenEstadoService.cancelar(ID_ORDEN, ID_DUENO))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigo()).isEqualTo("ORDEN_NO_CANCELABLE"));
    }

    /**
     * §5.6 criterio 3: P-14 sin resolver. La entidad ni siquiera expone un setter para
     * cargo_cancelacion, así que no hay forma de asignarlo desde el código.
     */
    @Test
    void criterio3ElCargoDeCancelacionNoSePuedeAsignarDesdeElCodigo() {
        assertThat(Arrays.stream(Orden.class.getMethods()).map(Method::getName))
                .as("P-14: la columna existe pero no debe aplicársele lógica de cobro")
                .doesNotContain("setCargoCancelacion");
    }

    /** RN-01: una orden ajena responde 404, no 403. */
    @Test
    void rn01UnOdontologoNoPuedeCancelarUnaOrdenAjena() {
        prepararOrdenEn(enProduccion);

        assertThatThrownBy(() -> ordenEstadoService.cancelar(ID_ORDEN, ID_INTRUSO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getHttpStatus().value()).isEqualTo(404))
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getCodigo()).isEqualTo("ORDEN_NO_ENCONTRADA"));

        verify(historialRepository, never()).saveAndFlush(any());
    }

    /** S-08 sin resolver: la cancelación no notifica, así que no publica evento. */
    @Test
    void s08LaCancelacionNoPublicaNingunEvento() {
        prepararOrdenEn(recibido);
        catalogoDevuelve(cancelado);

        ordenEstadoService.cancelar(ID_ORDEN, ID_DUENO);

        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void cancelarUnaOrdenInexistenteDevuelve404() {
        when(ordenRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenEstadoService.cancelar(404L, ID_DUENO))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(ordenService, never()).obtenerDetalle(anyLong(), anyLong(), anyBoolean());
    }
}
