package com.labgarcias.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.labgarcias.catalogos.domain.CodigoTipoOrden;
import com.labgarcias.catalogos.domain.Estado;
import com.labgarcias.catalogos.domain.TipoOrden;
import com.labgarcias.catalogos.domain.TipoTrabajo;
import com.labgarcias.catalogos.service.TipoOrdenService;
import com.labgarcias.catalogos.service.TipoTrabajoService;
import com.labgarcias.ordenes.domain.Orden;
import com.labgarcias.ordenes.domain.OrdenCreadaEvent;
import com.labgarcias.ordenes.domain.OrdenHistorialEstado;
import com.labgarcias.ordenes.dto.CrearOrdenRequest;
import com.labgarcias.ordenes.dto.OrdenResponse;
import com.labgarcias.ordenes.repository.OrdenHistorialEstadoRepository;
import com.labgarcias.ordenes.repository.OrdenRepository;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.shared.excepcion.ReglaNegocioException;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    @Mock
    private OrdenRepository ordenRepository;
    @Mock
    private OrdenHistorialEstadoRepository historialRepository;
    @Mock
    private TipoTrabajoService tipoTrabajoService;
    @Mock
    private TipoOrdenService tipoOrdenService;
    @Spy
    private FabricaOrden fabricaOrden = new FabricaOrden();
    @Mock
    private ApplicationEventPublisher eventos;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private OrdenService ordenService;

    private TipoTrabajo tipoTrabajo;
    private TipoOrden tipoOrdenNormal;

    private static final CrearOrdenRequest REQUEST =
            new CrearOrdenRequest("Martín Pérez", LocalDate.of(2026, 8, 6), 16, "NORMAL", "Disyuntor superior");

    @BeforeEach
    void prepararCatalogo() {
        tipoTrabajo = new TipoTrabajo();
        tipoTrabajo.setNombre("DISYUNTOR CON TORNILLO ESTANDAR");
        tipoTrabajo.setDiasEstimados((short) 7);
        tipoTrabajo.setPrecio(new BigDecimal("250.00"));
        tipoTrabajo.setActivo(true);

        Estado recibido = new Estado();
        recibido.setCodigo("RECIBIDO");
        recibido.setNombre("Recibido");
        tipoOrdenNormal = new TipoOrden();
        tipoOrdenNormal.setNombre("Normal");
        tipoOrdenNormal.setEstadoInicial(recibido);
        tipoOrdenNormal.setRecargoMonto(new BigDecimal("0.00"));
        tipoOrdenNormal.setNotificaAdmin(false);
    }

    /**
     * La orden que devuelve saveAndFlush simula lo que la base completó al insertar:
     * codigo, paciente_codigo y precio_total son columnas generadas sin setter.
     */
    private Orden ordenPersistida(Orden construida) {
        Orden persistida = mock(Orden.class);
        when(persistida.getId()).thenReturn(1L);
        when(persistida.getCodigo()).thenReturn("LG-0001");
        when(persistida.getPacienteCodigo()).thenReturn(1000);
        when(persistida.getPacienteIniciales()).thenReturn(construida.getPacienteIniciales());
        when(persistida.getEstado()).thenReturn(construida.getEstado());
        when(persistida.getTipoTrabajo()).thenReturn(construida.getTipoTrabajo());
        when(persistida.getTipoOrden()).thenReturn(construida.getTipoOrden());
        when(persistida.getDescripcion()).thenReturn(construida.getDescripcion());
        when(persistida.getFechaIngreso()).thenReturn(construida.getFechaIngreso());
        when(persistida.getFechaEstimadaEntrega()).thenReturn(construida.getFechaEstimadaEntrega());
        when(persistida.getPrecioBase()).thenReturn(construida.getPrecioBase());
        when(persistida.getRecargoUrgencia()).thenReturn(construida.getRecargoUrgencia());
        when(persistida.getPrecioTotal()).thenReturn(new BigDecimal("250.00"));
        return persistida;
    }

    private OrdenResponse crearOrdenNormal() {
        when(tipoTrabajoService.obtenerActivoParaOrden(16)).thenReturn(tipoTrabajo);
        when(tipoOrdenService.obtenerPorCodigo(CodigoTipoOrden.NORMAL)).thenReturn(tipoOrdenNormal);
        when(entityManager.getReference(Usuario.class, 7L)).thenReturn(new Usuario());
        when(ordenRepository.saveAndFlush(any(Orden.class)))
                .thenAnswer(invocacion -> ordenPersistida(invocacion.getArgument(0)));
        return ordenService.crear(REQUEST, 7L);
    }

    @Test
    void criterio4LaRespuestaNoContieneElNombreDelPaciente() {
        OrdenResponse respuesta = crearOrdenNormal();

        assertThat(respuesta.pacienteIdentificacion()).isEqualTo("M.P. - Caso #1000");
        assertThat(respuesta.toString()).doesNotContain("Martín Pérez");
    }

    @Test
    void criterio2ElPrecioTotalLoDevuelveLaBaseNoLoRecalculaElServicio() {
        OrdenResponse respuesta = crearOrdenNormal();

        assertThat(respuesta.precioBase()).isEqualByComparingTo("250.00");
        assertThat(respuesta.recargoUrgencia()).isEqualByComparingTo("0.00");
        assertThat(respuesta.precioTotal()).isEqualByComparingTo("250.00");
    }

    @Test
    void laRespuestaTraeLosDatosPublicosDeLaOrden() {
        OrdenResponse respuesta = crearOrdenNormal();

        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.codigo()).isEqualTo("LG-0001");
        assertThat(respuesta.estado()).isEqualTo("Recibido");
        assertThat(respuesta.tipoOrden()).isEqualTo("Normal");
        assertThat(respuesta.tipoTrabajo()).isEqualTo("DISYUNTOR CON TORNILLO ESTANDAR");
        assertThat(respuesta.fechaEstimadaEntrega()).isEqualTo(LocalDate.of(2026, 8, 17));
    }

    @Test
    void rn01LaOrdenSeAsociaAlOdontologoAutenticado() {
        crearOrdenNormal();

        verify(entityManager).getReference(Usuario.class, 7L);
    }

    @Test
    void registraElEstadoInicialEnElHistorialSinUsuarioAutor() {
        crearOrdenNormal();

        ArgumentCaptor<OrdenHistorialEstado> captor = ArgumentCaptor.forClass(OrdenHistorialEstado.class);
        verify(historialRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isNull();
        assertThat(captor.getValue().getEstado().getCodigo()).isEqualTo("RECIBIDO");
    }

    @Test
    void publicaOrdenCreadaEventConElNotificaAdminDeLaTabla() {
        crearOrdenNormal();

        ArgumentCaptor<OrdenCreadaEvent> captor = ArgumentCaptor.forClass(OrdenCreadaEvent.class);
        verify(eventos).publishEvent(captor.capture());
        OrdenCreadaEvent evento = captor.getValue();
        assertThat(evento.ordenId()).isEqualTo(1L);
        assertThat(evento.codigo()).isEqualTo("LG-0001");
        assertThat(evento.pacienteCodigo()).isEqualTo(1000);
        assertThat(evento.notificaAdmin()).isFalse();
    }

    @Test
    void rn11UnaOrdenUrgenteMarcaNotificaAdminSegunLaTabla() {
        Estado enEvaluacion = new Estado();
        enEvaluacion.setCodigo("EN_EVALUACION");
        enEvaluacion.setNombre("En evaluacion");
        TipoOrden urgente = new TipoOrden();
        urgente.setNombre("Urgente");
        urgente.setEstadoInicial(enEvaluacion);
        urgente.setRecargoMonto(new BigDecimal("200.00"));
        urgente.setNotificaAdmin(true);

        when(tipoTrabajoService.obtenerActivoParaOrden(16)).thenReturn(tipoTrabajo);
        when(tipoOrdenService.obtenerPorCodigo(CodigoTipoOrden.URGENTE)).thenReturn(urgente);
        when(entityManager.getReference(Usuario.class, 7L)).thenReturn(new Usuario());
        when(ordenRepository.saveAndFlush(any(Orden.class)))
                .thenAnswer(invocacion -> ordenPersistida(invocacion.getArgument(0)));

        CrearOrdenRequest urgenteRequest =
                new CrearOrdenRequest("Martín Pérez", LocalDate.of(2026, 8, 6), 16, "URGENTE", null);
        ordenService.crear(urgenteRequest, 7L);

        ArgumentCaptor<OrdenCreadaEvent> captor = ArgumentCaptor.forClass(OrdenCreadaEvent.class);
        verify(eventos).publishEvent(captor.capture());
        assertThat(captor.getValue().notificaAdmin()).isTrue();
    }

    @Test
    void unTipoDeTrabajoInactivoImpideCrearLaOrden() {
        when(tipoTrabajoService.obtenerActivoParaOrden(16))
                .thenThrow(new ReglaNegocioException("TIPO_TRABAJO_INACTIVO",
                        "El tipo de trabajo no existe o no está disponible.", "tipoTrabajoId"));

        assertThatThrownBy(() -> ordenService.crear(REQUEST, 7L))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo()).isEqualTo("TIPO_TRABAJO_INACTIVO"));

        verify(ordenRepository, never()).saveAndFlush(any());
        verify(historialRepository, never()).save(any());
        verify(eventos, never()).publishEvent(any(OrdenCreadaEvent.class));
    }
}
