package com.labgarcias.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.labgarcias.catalogos.domain.Estado;
import com.labgarcias.catalogos.domain.TipoOrden;
import com.labgarcias.catalogos.domain.TipoTrabajo;
import com.labgarcias.ordenes.domain.Orden;
import com.labgarcias.ordenes.dto.DashboardAdminResponse;
import com.labgarcias.ordenes.dto.PanelOdontologoResponse;
import com.labgarcias.ordenes.repository.DashboardRepository;
import com.labgarcias.ordenes.repository.DistribucionEstadoProyeccion;
import com.labgarcias.ordenes.repository.OrdenUrgenteProyeccion;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final long ID_ODONTOLOGO = 3L;
    private static final int FILAS_ESPERADAS_POR_BLOQUE = 5;

    @Mock
    private DashboardRepository dashboardRepository;
    @Mock
    private SemanaLaboratorio semanaLaboratorio;

    private DashboardService dashboardService;

    private SemanaLaboratorio.Rango semana;

    @BeforeEach
    void prepararSemana() {
        semana = new SemanaLaboratorio("America/Montevideo").semanaDe(LocalDate.of(2026, 8, 18));
        dashboardService = new DashboardService(dashboardRepository, new MapeadorOrden(), semanaLaboratorio);
    }

    private void conSemanaEnCurso() {
        when(semanaLaboratorio.semanaEnCurso()).thenReturn(semana);
    }

    /**
     * CU-02/RN-01: el panel del odontólogo consulta siempre con su id. Si alguna consulta se
     * llamara con null, ese contador pasaría a ser el de todo el laboratorio.
     */
    @Test
    void rn01ElPanelDelOdontologoConsultaSiempreConSuId() {
        conSemanaEnCurso();
        when(dashboardRepository.contarEnCurso("LISTO", ID_ODONTOLOGO)).thenReturn(3L);
        when(dashboardRepository.contarListasParaRetirar("LISTO", ID_ODONTOLOGO)).thenReturn(1L);
        when(dashboardRepository.contarEntregadasEntre(eq("ENTREGADO"), any(), any(), eq(ID_ODONTOLOGO)))
                .thenReturn(2L);
        when(dashboardRepository.buscarRecientes(eq(ID_ODONTOLOGO), any())).thenReturn(List.of());

        PanelOdontologoResponse panel = dashboardService.panelDelOdontologo(ID_ODONTOLOGO);

        assertThat(panel.contadores().enCurso()).isEqualTo(3);
        assertThat(panel.contadores().listasParaRetirar()).isEqualTo(1);
        assertThat(panel.contadores().entregadasEstaSemana()).isEqualTo(2);
        verify(dashboardRepository).contarEnCurso("LISTO", ID_ODONTOLOGO);
        verify(dashboardRepository).buscarRecientes(eq(ID_ODONTOLOGO), any());
    }

    /** D-11: CU-02 enumera "mensajes nuevos" y la respuesta no lo trae, porque la mensajería está pospuesta. */
    @Test
    void d11ElPanelNoTieneContadorDeMensajes() {
        assertThat(PanelOdontologoResponse.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("contadores", "ordenesRecientes");
        assertThat(com.labgarcias.ordenes.dto.ContadoresOdontologoResponse.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("enCurso", "listasParaRetirar", "entregadasEstaSemana");
    }

    /**
     * El corte de "entregadas esta semana" es el que arma `SemanaLaboratorio`: el service no
     * calcula fechas por su cuenta ni las convierte a otro huso.
     */
    @Test
    void lasEntregadasSeCuentanConElRangoDeLaSemanaDelLaboratorio() {
        conSemanaEnCurso();
        when(dashboardRepository.buscarRecientes(any(), any())).thenReturn(List.of());

        dashboardService.panelDelOdontologo(ID_ODONTOLOGO);

        ArgumentCaptor<OffsetDateTime> desde = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> hasta = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(dashboardRepository)
                .contarEntregadasEntre(eq("ENTREGADO"), desde.capture(), hasta.capture(), eq(ID_ODONTOLOGO));

        assertThat(desde.getValue()).isEqualTo(semana.desde());
        assertThat(hasta.getValue()).isEqualTo(semana.hasta());
    }

    /** CU-10: el dashboard del laboratorio consulta sin id de odontólogo, así abarca a todos. */
    @Test
    void cu10ElDashboardDelLaboratorioNoFiltraPorOdontologo() {
        conSemanaEnCurso();
        when(dashboardRepository.contarEnCurso(eq("LISTO"), isNull())).thenReturn(12L);
        when(dashboardRepository.contarListasParaRetirar(eq("LISTO"), isNull())).thenReturn(4L);
        when(dashboardRepository.contarEntregadasEntre(eq("ENTREGADO"), any(), any(), isNull())).thenReturn(7L);
        when(dashboardRepository.contarUrgentesActivas()).thenReturn(2L);
        when(dashboardRepository.buscarRecientes(isNull(), any())).thenReturn(List.of());
        when(dashboardRepository.buscarProximasAEntregar(any())).thenReturn(List.of());
        when(dashboardRepository.distribucionPorEstado()).thenReturn(List.of());
        when(dashboardRepository.buscarUrgentes(FILAS_ESPERADAS_POR_BLOQUE)).thenReturn(List.of());

        DashboardAdminResponse dashboard = dashboardService.dashboardDelLaboratorio();

        assertThat(dashboard.contadores().enCurso()).isEqualTo(12);
        assertThat(dashboard.contadores().listasParaRetirar()).isEqualTo(4);
        assertThat(dashboard.contadores().entregadasEstaSemana()).isEqualTo(7);
        assertThat(dashboard.contadores().urgentesActivas()).isEqualTo(2);
    }

    /** §5.7: los cuatro bloques del dashboard traen hasta 5 filas, y cada uno con su orden. */
    @Test
    void cadaBloqueDeResumenPideCincoFilasConSuOrden() {
        conSemanaEnCurso();
        when(dashboardRepository.buscarRecientes(isNull(), any())).thenReturn(List.of());
        when(dashboardRepository.buscarProximasAEntregar(any())).thenReturn(List.of());
        when(dashboardRepository.distribucionPorEstado()).thenReturn(List.of());
        when(dashboardRepository.buscarUrgentes(FILAS_ESPERADAS_POR_BLOQUE)).thenReturn(List.of());

        dashboardService.dashboardDelLaboratorio();

        ArgumentCaptor<Pageable> recientes = ArgumentCaptor.forClass(Pageable.class);
        verify(dashboardRepository).buscarRecientes(isNull(), recientes.capture());
        assertThat(recientes.getValue().getPageSize()).isEqualTo(FILAS_ESPERADAS_POR_BLOQUE);
        assertThat(recientes.getValue().getSort().getOrderFor("fechaIngreso").getDirection())
                .isEqualTo(Sort.Direction.DESC);

        ArgumentCaptor<Pageable> proximas = ArgumentCaptor.forClass(Pageable.class);
        verify(dashboardRepository).buscarProximasAEntregar(proximas.capture());
        assertThat(proximas.getValue().getPageSize()).isEqualTo(FILAS_ESPERADAS_POR_BLOQUE);
        assertThat(proximas.getValue().getSort().getOrderFor("fechaEstimadaEntrega").getDirection())
                .as("§5.7: próximas a entregar van por fecha estimada, la más cercana primero")
                .isEqualTo(Sort.Direction.ASC);

        verify(dashboardRepository).buscarUrgentes(FILAS_ESPERADAS_POR_BLOQUE);
    }

    /** §5.7: la distribución sale de la vista tal cual, con código y nombre. */
    @Test
    void laDistribucionPorEstadoLlevaCodigoYNombre() {
        conSemanaEnCurso();
        when(dashboardRepository.buscarRecientes(isNull(), any())).thenReturn(List.of());
        when(dashboardRepository.buscarProximasAEntregar(any())).thenReturn(List.of());
        when(dashboardRepository.buscarUrgentes(FILAS_ESPERADAS_POR_BLOQUE)).thenReturn(List.of());
        DistribucionEstadoProyeccion enProduccion = distribucion("EN_PRODUCCION", "En producción", 5);
        when(dashboardRepository.distribucionPorEstado()).thenReturn(List.of(enProduccion));

        DashboardAdminResponse dashboard = dashboardService.dashboardDelLaboratorio();

        assertThat(dashboard.distribucionPorEstado()).singleElement().satisfies(fila -> {
            assertThat(fila.estadoCodigo()).isEqualTo("EN_PRODUCCION");
            assertThat(fila.estadoNombre()).isEqualTo("En producción");
            assertThat(fila.cantidad()).isEqualTo(5);
        });
    }

    /**
     * RN-22 / `Agente.md` §8.2: `v_ordenes_urgentes` incluye `paciente_nombre` y el bloque del
     * dashboard no lo expone. La proyección ni siquiera declara el campo, así que el dato no sale
     * de la base.
     */
    @Test
    void rn22ElBloqueDeUrgentesNoExponeElNombreDelPaciente() {
        conSemanaEnCurso();
        when(dashboardRepository.buscarRecientes(isNull(), any())).thenReturn(List.of());
        when(dashboardRepository.buscarProximasAEntregar(any())).thenReturn(List.of());
        when(dashboardRepository.distribucionPorEstado()).thenReturn(List.of());
        OrdenUrgenteProyeccion urgente = urgente();
        when(dashboardRepository.buscarUrgentes(FILAS_ESPERADAS_POR_BLOQUE)).thenReturn(List.of(urgente));

        DashboardAdminResponse dashboard = dashboardService.dashboardDelLaboratorio();

        assertThat(dashboard.urgentes()).singleElement().satisfies(fila -> {
            assertThat(fila.codigo()).isEqualTo("LG-0007");
            assertThat(fila.odontologo()).isEqualTo("Dr. Juan Pérez");
            assertThat(fila.toString()).doesNotContain("Martín Pérez");
        });
        assertThat(OrdenUrgenteProyeccion.class.getMethods())
                .extracting(java.lang.reflect.Method::getName)
                .as("RN-22: la proyección no puede tener un getter del nombre del paciente")
                .doesNotContain("getPacienteNombre");
    }

    /** RN-22: las órdenes recientes identifican al paciente por iniciales y código, como todo listado. */
    @Test
    void rn22LasRecientesIdentificanAlPacientePorInicialesYCodigo() {
        conSemanaEnCurso();
        Orden reciente = orden();
        when(dashboardRepository.buscarRecientes(eq(ID_ODONTOLOGO), any())).thenReturn(List.of(reciente));

        PanelOdontologoResponse panel = dashboardService.panelDelOdontologo(ID_ODONTOLOGO);

        assertThat(panel.ordenesRecientes()).singleElement().satisfies(item -> {
            assertThat(item.pacienteIdentificacion()).isEqualTo("M.P. - Caso #1000");
            assertThat(item.toString()).doesNotContain("Martín Pérez");
        });
    }

    private DistribucionEstadoProyeccion distribucion(String codigo, String nombre, long cantidad) {
        DistribucionEstadoProyeccion fila = mock(DistribucionEstadoProyeccion.class);
        when(fila.getEstadoCodigo()).thenReturn(codigo);
        when(fila.getEstadoNombre()).thenReturn(nombre);
        when(fila.getCantidad()).thenReturn(cantidad);
        return fila;
    }

    private OrdenUrgenteProyeccion urgente() {
        OrdenUrgenteProyeccion fila = mock(OrdenUrgenteProyeccion.class);
        when(fila.getId()).thenReturn(7L);
        when(fila.getCodigo()).thenReturn("LG-0007");
        when(fila.getOdontologo()).thenReturn("Dr. Juan Pérez");
        when(fila.getEstado()).thenReturn("En producción");
        when(fila.getFechaEstimadaEntrega()).thenReturn(LocalDate.of(2026, 8, 28));
        return fila;
    }

    private Orden orden() {
        TipoTrabajo tipoTrabajo = new TipoTrabajo();
        tipoTrabajo.setNombre("DISYUNTOR CON TORNILLO ESTANDAR");
        TipoOrden tipoOrden = new TipoOrden();
        tipoOrden.setNombre("Normal");
        Estado estado = new Estado();
        estado.setNombre("En producción");

        Orden orden = mock(Orden.class);
        when(orden.getPacienteIniciales()).thenReturn("M.P.");
        when(orden.getPacienteCodigo()).thenReturn(1000);
        when(orden.getTipoTrabajo()).thenReturn(tipoTrabajo);
        when(orden.getTipoOrden()).thenReturn(tipoOrden);
        when(orden.getEstado()).thenReturn(estado);
        when(orden.getPrecioTotal()).thenReturn(new BigDecimal("250.00"));
        return orden;
    }
}
