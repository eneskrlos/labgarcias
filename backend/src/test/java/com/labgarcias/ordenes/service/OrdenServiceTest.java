package com.labgarcias.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
import com.labgarcias.ordenes.dto.OrdenArchivoResponse;
import com.labgarcias.ordenes.dto.OrdenDetalleResponse;
import com.labgarcias.ordenes.dto.OrdenListadoResponse;
import com.labgarcias.ordenes.dto.OrdenResponse;
import com.labgarcias.ordenes.repository.OrdenHistorialEstadoRepository;
import com.labgarcias.ordenes.repository.OrdenRepository;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.service.UsuarioService;
import com.labgarcias.shared.dto.PaginaResponse;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;
import com.labgarcias.shared.excepcion.ReglaNegocioException;
import com.labgarcias.shared.excepcion.ValidacionException;

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
    private OrdenArchivoService ordenArchivoService;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private ApplicationEventPublisher eventos;

    @InjectMocks
    private OrdenService ordenService;

    private TipoTrabajo tipoTrabajo;
    private TipoOrden tipoOrdenNormal;

    private static final long ID_ORDEN = 1L;
    private static final long ID_DUENO = 7L;
    private static final long ID_INTRUSO = 99L;

    private static final CrearOrdenRequest REQUEST =
            new CrearOrdenRequest(ID_DUENO, "Martín Pérez", LocalDate.of(2026, 8, 6), 16, "NORMAL", "Disyuntor superior");

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
        when(usuarioService.obtenerOdontologoActivoParaOrden(ID_DUENO)).thenReturn(new Usuario());
        when(ordenRepository.saveAndFlush(any(Orden.class)))
                .thenAnswer(invocacion -> ordenPersistida(invocacion.getArgument(0)));
        return ordenService.crear(REQUEST);
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

    /** D-19: la orden queda a nombre del odontologoId del request, validado como cuenta activa. */
    @Test
    void d19LaOrdenSeAsociaAlOdontologoIndicadoEnElRequest() {
        crearOrdenNormal();

        verify(usuarioService).obtenerOdontologoActivoParaOrden(ID_DUENO);
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
        when(usuarioService.obtenerOdontologoActivoParaOrden(ID_DUENO)).thenReturn(new Usuario());
        when(ordenRepository.saveAndFlush(any(Orden.class)))
                .thenAnswer(invocacion -> ordenPersistida(invocacion.getArgument(0)));

        CrearOrdenRequest urgenteRequest =
                new CrearOrdenRequest(ID_DUENO, "Martín Pérez", LocalDate.of(2026, 8, 6), 16, "URGENTE", null);
        ordenService.crear(urgenteRequest);

        ArgumentCaptor<OrdenCreadaEvent> captor = ArgumentCaptor.forClass(OrdenCreadaEvent.class);
        verify(eventos).publishEvent(captor.capture());
        assertThat(captor.getValue().notificaAdmin()).isTrue();
    }

    @Test
    void unTipoDeTrabajoInactivoImpideCrearLaOrden() {
        when(tipoTrabajoService.obtenerActivoParaOrden(16))
                .thenThrow(new ReglaNegocioException("TIPO_TRABAJO_INACTIVO",
                        "El tipo de trabajo no existe o no está disponible.", "tipoTrabajoId"));

        assertThatThrownBy(() -> ordenService.crear(REQUEST))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo()).isEqualTo("TIPO_TRABAJO_INACTIVO"));

        verify(ordenRepository, never()).saveAndFlush(any());
        verify(historialRepository, never()).save(any());
        verify(eventos, never()).publishEvent(any(OrdenCreadaEvent.class));
    }

    // --- CU-03 / CU-04: listado, detalle y seguimiento (T-19) ---

    /**
     * Fixture del detalle. Las stubs van lenient porque no todos los tests llegan a leer
     * la orden entera: el que verifica RN-01 corta en el dueño y no mapea nada.
     */
    private Orden ordenCompleta() {
        Usuario dueno = mock(Usuario.class);
        lenient().when(dueno.getId()).thenReturn(ID_DUENO);

        Estado enProduccion = new Estado();
        enProduccion.setCodigo("EN_PRODUCCION");
        enProduccion.setNombre("En producción");

        Orden orden = mock(Orden.class);
        lenient().when(orden.getId()).thenReturn(ID_ORDEN);
        lenient().when(orden.getCodigo()).thenReturn("LG-0001");
        lenient().when(orden.getOdontologo()).thenReturn(dueno);
        lenient().when(orden.getPacienteNombre()).thenReturn("Martín Pérez");
        lenient().when(orden.getPacienteIniciales()).thenReturn("M.P.");
        lenient().when(orden.getPacienteCodigo()).thenReturn(1000);
        lenient().when(orden.getTipoTrabajo()).thenReturn(tipoTrabajo);
        lenient().when(orden.getTipoOrden()).thenReturn(tipoOrdenNormal);
        lenient().when(orden.getEstado()).thenReturn(enProduccion);
        lenient().when(orden.getDescripcion()).thenReturn("Disyuntor superior");
        lenient().when(orden.getFechaIngreso()).thenReturn(LocalDate.of(2026, 8, 6));
        lenient().when(orden.getFechaEstimadaEntrega()).thenReturn(LocalDate.of(2026, 8, 17));
        lenient().when(orden.getPrecioBase()).thenReturn(new BigDecimal("250.00"));
        lenient().when(orden.getRecargoUrgencia()).thenReturn(new BigDecimal("0.00"));
        lenient().when(orden.getPrecioTotal()).thenReturn(new BigDecimal("250.00"));
        return orden;
    }

    private OrdenHistorialEstado etapa(String codigo, String nombre, OffsetDateTime fechaHora, Usuario autor) {
        Estado estado = new Estado();
        estado.setCodigo(codigo);
        estado.setNombre(nombre);
        OrdenHistorialEstado registro = mock(OrdenHistorialEstado.class);
        when(registro.getEstado()).thenReturn(estado);
        when(registro.getFechaHora()).thenReturn(fechaHora);
        when(registro.getUsuario()).thenReturn(autor);
        return registro;
    }

    /**
     * Los mocks se arman antes del when(): construirlos dentro de thenReturn() interrumpe
     * el stubbing en curso y Mockito lo rechaza con UnfinishedStubbing.
     */
    private OrdenDetalleResponse detalleComo(long usuarioId, boolean esAdministrador) {
        Orden orden = ordenCompleta();
        when(ordenRepository.buscarParaDetalle(ID_ORDEN)).thenReturn(Optional.of(orden));
        return ordenService.obtenerDetalle(ID_ORDEN, usuarioId, esAdministrador);
    }

    /** §5.4 criterio 1: cada etapa con su fecha y hora, en orden cronológico. */
    @Test
    void criterio1LaLineaDeTiempoMuestraCadaEtapaConFechaYHora() {
        OffsetDateTime alta = OffsetDateTime.parse("2026-08-06T09:00:00-03:00");
        OffsetDateTime avance = OffsetDateTime.parse("2026-08-08T10:15:30-03:00");
        Usuario tecnica = mock(Usuario.class);
        when(tecnica.getNombreCompleto()).thenReturn("Laura García");
        List<OrdenHistorialEstado> historial = List.of(
                etapa("RECIBIDO", "Recibido", alta, null),
                etapa("EN_PRODUCCION", "En producción", avance, tecnica));
        when(historialRepository.findByOrdenIdOrderByFechaHoraAsc(ID_ORDEN)).thenReturn(historial);

        OrdenDetalleResponse detalle = detalleComo(ID_DUENO, false);

        assertThat(detalle.lineaTiempo()).hasSize(2);
        assertThat(detalle.lineaTiempo().get(0).estado()).isEqualTo("Recibido");
        assertThat(detalle.lineaTiempo().get(0).fechaHora()).isEqualTo(alta);
        assertThat(detalle.lineaTiempo().get(1).fechaHora()).isEqualTo(avance);
        assertThat(detalle.lineaTiempo().get(1).autor()).isEqualTo("Laura García");
    }

    /** §5.1 paso 9: el registro inicial lo asigna el sistema, así que no tiene autor. */
    @Test
    void laEtapaInicialAsignadaPorElSistemaNoTieneAutor() {
        List<OrdenHistorialEstado> historial =
                List.of(etapa("RECIBIDO", "Recibido", OffsetDateTime.parse("2026-08-06T09:00:00-03:00"), null));
        when(historialRepository.findByOrdenIdOrderByFechaHoraAsc(ID_ORDEN)).thenReturn(historial);

        OrdenDetalleResponse detalle = detalleComo(ID_DUENO, false);

        assertThat(detalle.lineaTiempo().get(0).autor()).isNull();
    }

    /** §5.4 criterio 2: RN-01 responde 404, no 403, para no revelar que la orden existe. */
    @Test
    void criterio2UnOdontologoRecibe404AlPedirUnaOrdenAjena() {
        Orden orden = ordenCompleta();
        when(ordenRepository.buscarParaDetalle(ID_ORDEN)).thenReturn(Optional.of(orden));

        assertThatThrownBy(() -> ordenService.obtenerDetalle(ID_ORDEN, ID_INTRUSO, false))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getHttpStatus().value()).isEqualTo(404))
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getCodigo()).isEqualTo("ORDEN_NO_ENCONTRADA"));

        verify(historialRepository, never()).findByOrdenIdOrderByFechaHoraAsc(any());
    }

    /** §5.4 criterio 3: RN-22, el odontólogo nunca ve el nombre del paciente. */
    @Test
    void criterio3LaRespuestaAlOdontologoNoContieneElNombreDelPaciente() {
        OrdenDetalleResponse detalle = detalleComo(ID_DUENO, false);

        assertThat(detalle.pacienteNombre()).isNull();
        assertThat(detalle.pacienteIdentificacion()).isEqualTo("M.P. - Caso #1000");
        assertThat(detalle.toString()).doesNotContain("Martín Pérez");
    }

    /** RN-22: el laboratorio sí lo recibe, porque lo necesita para operar. */
    @Test
    void elAdministradorSiRecibeElNombreDelPaciente() {
        OrdenDetalleResponse detalle = detalleComo(ID_INTRUSO, true);

        assertThat(detalle.pacienteNombre()).isEqualTo("Martín Pérez");
    }

    @Test
    void elDetalleTraeElDesgloseDePreciosYLosAdjuntos() {
        List<OrdenArchivoResponse> adjuntos = List.of(new OrdenArchivoResponse(
                5L, "radiografia.jpg", "IMAGEN", "image/jpeg", 1024L,
                OffsetDateTime.parse("2026-08-06T09:05:00-03:00")));
        when(ordenArchivoService.listar(ID_ORDEN, ID_DUENO, false)).thenReturn(adjuntos);

        OrdenDetalleResponse detalle = detalleComo(ID_DUENO, false);

        assertThat(detalle.precioBase()).isEqualByComparingTo("250.00");
        assertThat(detalle.recargoUrgencia()).isEqualByComparingTo("0.00");
        assertThat(detalle.precioTotal()).isEqualByComparingTo("250.00");
        assertThat(detalle.descripcion()).isEqualTo("Disyuntor superior");
        assertThat(detalle.archivos()).containsExactlyElementsOf(adjuntos);
    }

    @Test
    void unaOrdenInexistenteDevuelve404() {
        when(ordenRepository.buscarParaDetalle(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenService.obtenerDetalle(404L, ID_DUENO, false))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    /** §5.3/RN-01: el id del dueño sale del token, nunca de la petición. */
    @Test
    void rn01ElListadoConsultaSiempreConElIdDelOdontologoAutenticado() {
        Pageable pagina = PageRequest.of(0, 10);
        PageImpl<Orden> resultado = new PageImpl<>(List.of(ordenCompleta()), pagina, 1);
        when(ordenRepository.buscarDelOdontologo(ID_DUENO, null, pagina)).thenReturn(resultado);

        PaginaResponse<OrdenListadoResponse> respuesta = ordenService.listarMisOrdenes(ID_DUENO, null, pagina);

        verify(ordenRepository).buscarDelOdontologo(ID_DUENO, null, pagina);
        assertThat(respuesta.total()).isEqualTo(1);
        assertThat(respuesta.contenido()).hasSize(1);
    }

    /** RN-22: tampoco el listado expone el nombre; identifica por iniciales y código. */
    @Test
    void elListadoIdentificaAlPacientePorInicialesYCodigo() {
        Pageable pagina = PageRequest.of(0, 10);
        PageImpl<Orden> resultado = new PageImpl<>(List.of(ordenCompleta()), pagina, 1);
        when(ordenRepository.buscarDelOdontologo(ID_DUENO, null, pagina)).thenReturn(resultado);

        OrdenListadoResponse item = ordenService.listarMisOrdenes(ID_DUENO, null, pagina).contenido().get(0);

        assertThat(item.pacienteIdentificacion()).isEqualTo("M.P. - Caso #1000");
        assertThat(item.toString()).doesNotContain("Martín Pérez");
        assertThat(item.codigo()).isEqualTo("LG-0001");
        assertThat(item.estado()).isEqualTo("En producción");
        assertThat(item.precioTotal()).isEqualByComparingTo("250.00");
    }

    /** El filtro opcional viaja tal cual al repositorio, que compara contra estado.codigo. */
    @Test
    void elFiltroPorEstadoSePasaAlRepositorio() {
        Pageable pagina = PageRequest.of(0, 20);
        when(ordenRepository.buscarDelOdontologo(ID_DUENO, "EN_PRODUCCION", pagina))
                .thenReturn(new PageImpl<>(List.of(), pagina, 0));

        ordenService.listarMisOrdenes(ID_DUENO, "EN_PRODUCCION", pagina);

        verify(ordenRepository).buscarDelOdontologo(ID_DUENO, "EN_PRODUCCION", pagina);
    }

    /** spec.md §1: size solo admite 10, 20 o 30. */
    @Test
    void unTamanoDePaginaNoPermitidoEsRechazado() {
        Pageable pagina = PageRequest.of(0, 15);

        assertThatThrownBy(() -> ordenService.listarMisOrdenes(ID_DUENO, null, pagina))
                .isInstanceOf(ValidacionException.class)
                .satisfies(ex -> assertThat(((ValidacionException) ex).getCodigo()).isEqualTo("TAMANO_PAGINA_INVALIDO"));

        verify(ordenRepository, never()).buscarDelOdontologo(any(), any(), any());
    }
}
