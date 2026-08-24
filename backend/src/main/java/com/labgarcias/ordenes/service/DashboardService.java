package com.labgarcias.ordenes.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.ordenes.dto.ContadoresLaboratorioResponse;
import com.labgarcias.ordenes.dto.ContadoresOdontologoResponse;
import com.labgarcias.ordenes.dto.DashboardAdminResponse;
import com.labgarcias.ordenes.dto.DistribucionEstadoResponse;
import com.labgarcias.ordenes.dto.OrdenListadoResponse;
import com.labgarcias.ordenes.dto.OrdenUrgenteResponse;
import com.labgarcias.ordenes.dto.PanelOdontologoResponse;
import com.labgarcias.ordenes.repository.DashboardRepository;
import com.labgarcias.ordenes.service.SemanaLaboratorio.Rango;

/**
 * CU-02 y CU-10/§5.7: los dos paneles. Solo lectura y agregación: acá no cambia nada de estado.
 *
 * Los dos comparten los tres primeros indicadores, que se calculan con la misma consulta y se
 * diferencian por un único argumento: el id del odontólogo para su panel (RN-01) o `null` para el
 * laboratorio. **Todo lo que se muestra lo cuenta la base** — §8 prohíbe que el cliente calcule
 * nada, y traer órdenes para contarlas en memoria sería mover el mismo problema al servidor.
 */
@Service
public class DashboardService {

    /**
     * CU-02/CU-10, "listas para retirar": la etapa `LISTO` del catálogo. Se identifica por su
     * código, que es estable, y no por su nombre, que CU-22 deja editar.
     */
    private static final String CODIGO_LISTO = "LISTO";

    /**
     * CU-02/CU-10, "entregadas esta semana": la entrega real es el pasaje a `ENTREGADO` en el
     * historial. La tabla `orden` no tiene columna de fecha de entrega.
     */
    private static final String CODIGO_ENTREGADO = "ENTREGADO";

    /**
     * §5.7: filas por bloque de resumen. No son listados paginados de §8.1 —el panel es un
     * vistazo, no una tabla operable—: quien quiera la lista completa va a "Mis trabajos" o a
     * "Órdenes".
     */
    private static final int FILAS_POR_BLOQUE = 5;

    /** CU-02/CU-10: "recientes" es por fecha de ingreso, las últimas primero. */
    private static final Pageable RECIENTES =
            PageRequest.of(0, FILAS_POR_BLOQUE, Sort.by(Sort.Direction.DESC, "fechaIngreso"));

    /** §5.7: "próximas a entregar, ordenadas por fecha_estimada_entrega". */
    private static final Pageable PROXIMAS_A_ENTREGAR =
            PageRequest.of(0, FILAS_POR_BLOQUE, Sort.by(Sort.Direction.ASC, "fechaEstimadaEntrega"));

    /** RN-01 al revés: sin id de odontólogo, la consulta abarca todo el laboratorio (CU-10). */
    private static final Long TODO_EL_LABORATORIO = null;

    private final DashboardRepository dashboardRepository;
    private final MapeadorOrden mapeadorOrden;
    private final SemanaLaboratorio semanaLaboratorio;

    public DashboardService(DashboardRepository dashboardRepository,
                            MapeadorOrden mapeadorOrden,
                            SemanaLaboratorio semanaLaboratorio) {
        this.dashboardRepository = dashboardRepository;
        this.mapeadorOrden = mapeadorOrden;
        this.semanaLaboratorio = semanaLaboratorio;
    }

    /**
     * CU-02: el panel del odontólogo. El id llega desde el token, nunca desde la petición (RN-01):
     * no hay forma de pedir el panel de otro.
     */
    @Transactional(readOnly = true)
    public PanelOdontologoResponse panelDelOdontologo(Long odontologoId) {
        return new PanelOdontologoResponse(
                contadoresDe(odontologoId),
                recientesDe(odontologoId));
    }

    /** CU-10/§5.7: el dashboard del laboratorio, sobre las órdenes de todos los odontólogos. */
    @Transactional(readOnly = true)
    public DashboardAdminResponse dashboardDelLaboratorio() {
        return new DashboardAdminResponse(
                contadoresDelLaboratorio(),
                distribucionPorEstado(),
                proximasAEntregar(),
                recientesDe(TODO_EL_LABORATORIO),
                urgentes());
    }

    /**
     * Los tres indicadores que CU-02 y CU-10 comparten. Con `odontologoId` cuentan lo de ese
     * odontólogo; con `null`, lo de todo el laboratorio.
     */
    private ContadoresOdontologoResponse contadoresDe(Long odontologoId) {
        Rango semana = semanaLaboratorio.semanaEnCurso();
        return new ContadoresOdontologoResponse(
                dashboardRepository.contarEnCurso(CODIGO_LISTO, odontologoId),
                dashboardRepository.contarListasParaRetirar(CODIGO_LISTO, odontologoId),
                dashboardRepository.contarEntregadasEntre(
                        CODIGO_ENTREGADO, semana.desde(), semana.hasta(), odontologoId));
    }

    /** CU-10: los tres comunes más las urgentes activas, que son solo del laboratorio. */
    private ContadoresLaboratorioResponse contadoresDelLaboratorio() {
        ContadoresOdontologoResponse comunes = contadoresDe(TODO_EL_LABORATORIO);
        return new ContadoresLaboratorioResponse(
                comunes.enCurso(),
                comunes.listasParaRetirar(),
                comunes.entregadasEstaSemana(),
                dashboardRepository.contarUrgentesActivas());
    }

    private List<OrdenListadoResponse> recientesDe(Long odontologoId) {
        return dashboardRepository.buscarRecientes(odontologoId, RECIENTES).stream()
                .map(mapeadorOrden::aItemDeListado)
                .toList();
    }

    private List<OrdenListadoResponse> proximasAEntregar() {
        return dashboardRepository.buscarProximasAEntregar(PROXIMAS_A_ENTREGAR).stream()
                .map(mapeadorOrden::aItemDeListado)
                .toList();
    }

    private List<DistribucionEstadoResponse> distribucionPorEstado() {
        return dashboardRepository.distribucionPorEstado().stream()
                .map(fila -> new DistribucionEstadoResponse(
                        fila.getEstadoCodigo(), fila.getEstadoNombre(), fila.getCantidad()))
                .toList();
    }

    /** RN-22: la vista trae `paciente_nombre` y ni la consulta ni esta conversión lo tocan. */
    private List<OrdenUrgenteResponse> urgentes() {
        return dashboardRepository.buscarUrgentes(FILAS_POR_BLOQUE).stream()
                .map(fila -> new OrdenUrgenteResponse(
                        fila.getId(),
                        fila.getCodigo(),
                        fila.getOdontologo(),
                        fila.getEstado(),
                        fila.getFechaEstimadaEntrega()))
                .toList();
    }
}
