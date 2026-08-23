package com.labgarcias.ordenes.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.catalogos.domain.CodigoTipoOrden;
import com.labgarcias.catalogos.domain.TipoOrden;
import com.labgarcias.catalogos.domain.TipoTrabajo;
import com.labgarcias.catalogos.service.TipoOrdenService;
import com.labgarcias.catalogos.service.TipoTrabajoService;
import com.labgarcias.ordenes.domain.Orden;
import com.labgarcias.ordenes.domain.OrdenCreadaEvent;
import com.labgarcias.ordenes.domain.OrdenHistorialEstado;
import com.labgarcias.ordenes.dto.CrearOrdenRequest;
import com.labgarcias.ordenes.dto.EtapaSeguimientoResponse;
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
import com.labgarcias.shared.util.ValidadorPaginacion;

/**
 * CU-09: creación de órdenes. Los valores derivados los calcula el backend, nunca el cliente.
 * CU-03/CU-04: listado y detalle, siempre acotados al dueño salvo que consulte el laboratorio.
 */
@Service
public class OrdenService {

    private final OrdenRepository ordenRepository;
    private final OrdenHistorialEstadoRepository historialRepository;
    private final TipoTrabajoService tipoTrabajoService;
    private final TipoOrdenService tipoOrdenService;
    private final FabricaOrden fabricaOrden;
    private final OrdenArchivoService ordenArchivoService;
    private final UsuarioService usuarioService;
    private final ApplicationEventPublisher eventos;

    public OrdenService(OrdenRepository ordenRepository,
                        OrdenHistorialEstadoRepository historialRepository,
                        TipoTrabajoService tipoTrabajoService,
                        TipoOrdenService tipoOrdenService,
                        FabricaOrden fabricaOrden,
                        OrdenArchivoService ordenArchivoService,
                        UsuarioService usuarioService,
                        ApplicationEventPublisher eventos) {
        this.ordenRepository = ordenRepository;
        this.historialRepository = historialRepository;
        this.tipoTrabajoService = tipoTrabajoService;
        this.tipoOrdenService = tipoOrdenService;
        this.fabricaOrden = fabricaOrden;
        this.ordenArchivoService = ordenArchivoService;
        this.usuarioService = usuarioService;
        this.eventos = eventos;
    }

    @Transactional
    public OrdenResponse crear(CrearOrdenRequest request) {
        TipoTrabajo tipoTrabajo = tipoTrabajoService.obtenerActivoParaOrden(request.tipoTrabajoId());
        TipoOrden tipoOrden = tipoOrdenService.obtenerPorCodigo(CodigoTipoOrden.valueOf(request.tipoOrdenCodigo()));
        // D-19: la orden se registra a nombre del odontólogo indicado, no de quien está autenticado.
        Usuario odontologo = usuarioService.obtenerOdontologoActivoParaOrden(request.odontologoId());

        Orden orden = fabricaOrden.crear(request, odontologo, tipoTrabajo, tipoOrden);
        // saveAndFlush: codigo, paciente_codigo y precio_total los genera la base al insertar.
        Orden persistida = ordenRepository.saveAndFlush(orden);
        historialRepository.save(fabricaOrden.registroInicialDe(persistida));

        eventos.publishEvent(new OrdenCreadaEvent(
                persistida.getId(), persistida.getCodigo(), odontologo.getId(),
                persistida.getPacienteCodigo(), tipoOrden.isNotificaAdmin()));

        return aRespuesta(persistida);
    }

    /**
     * CU-03/§5.3: las órdenes del odontólogo autenticado. El id del dueño llega como argumento
     * desde el token, nunca como parámetro de la petición (RN-01): no hay forma de pedir las
     * órdenes de otro. El filtro por estado es opcional y compara contra estado.codigo.
     */
    @Transactional(readOnly = true)
    public PaginaResponse<OrdenListadoResponse> listarMisOrdenes(Long odontologoId, String estado, Pageable pageable) {
        ValidadorPaginacion.validarTamano(pageable.getPageSize());
        return PaginaResponse.de(
                ordenRepository.buscarDelOdontologo(odontologoId, estado, pageable).map(this::aItemDeListado));
    }

    /**
     * CU-04/§5.4: detalle con adjuntos y línea de tiempo.
     * RN-01: una orden ajena responde 404, no 403, para no revelar que existe.
     */
    @Transactional(readOnly = true)
    public OrdenDetalleResponse obtenerDetalle(Long ordenId, Long usuarioId, boolean esAdministrador) {
        Orden orden = ordenRepository.buscarParaDetalle(ordenId).orElseThrow(this::ordenNoEncontrada);
        if (!esAdministrador && !orden.getOdontologo().getId().equals(usuarioId)) {
            throw ordenNoEncontrada();
        }
        // listar() vuelve a verificar el acceso por su cuenta. La repetición es deliberada:
        // cada service se defiende solo, así que aflojar uno no abre el otro.
        List<OrdenArchivoResponse> archivos = ordenArchivoService.listar(ordenId, usuarioId, esAdministrador);
        List<EtapaSeguimientoResponse> lineaTiempo = historialRepository
                .findByOrdenIdOrderByFechaHoraAsc(ordenId).stream()
                .map(this::aEtapa)
                .toList();

        return new OrdenDetalleResponse(
                orden.getId(),
                orden.getCodigo(),
                identificacionPaciente(orden),
                // RN-22: el nombre del paciente solo viaja al laboratorio, que lo necesita para operar.
                esAdministrador ? orden.getPacienteNombre() : null,
                orden.getTipoTrabajo().getNombre(),
                orden.getTipoOrden().getNombre(),
                orden.getEstado().getNombre(),
                orden.getDescripcion(),
                orden.getFechaIngreso(),
                orden.getFechaEstimadaEntrega(),
                orden.getPrecioBase(),
                orden.getRecargoUrgencia(),
                orden.getPrecioTotal(),
                archivos,
                lineaTiempo);
    }

    private OrdenListadoResponse aItemDeListado(Orden orden) {
        return new OrdenListadoResponse(
                orden.getId(),
                orden.getCodigo(),
                identificacionPaciente(orden),
                orden.getTipoTrabajo().getNombre(),
                orden.getTipoOrden().getNombre(),
                orden.getEstado().getNombre(),
                orden.getFechaIngreso(),
                orden.getFechaEstimadaEntrega(),
                orden.getPrecioTotal());
    }

    private EtapaSeguimientoResponse aEtapa(OrdenHistorialEstado etapa) {
        Usuario autor = etapa.getUsuario();
        return new EtapaSeguimientoResponse(
                etapa.getEstado().getNombre(),
                etapa.getFechaHora(),
                // §5.1 paso 9: el registro inicial lo asigna el sistema y no tiene autor.
                autor == null ? null : autor.getNombreCompleto());
    }

    /** RN-03/RN-22: al paciente se lo identifica por iniciales y código, nunca por su nombre. */
    private String identificacionPaciente(Orden orden) {
        return orden.getPacienteIniciales() + " - Caso #" + orden.getPacienteCodigo();
    }

    private RecursoNoEncontradoException ordenNoEncontrada() {
        return new RecursoNoEncontradoException("ORDEN_NO_ENCONTRADA", "No existe la orden solicitada.");
    }

    /** RN-03/RN-22: la respuesta se arma con iniciales + código, nunca con el nombre del paciente. */
    private OrdenResponse aRespuesta(Orden orden) {
        return new OrdenResponse(
                orden.getId(),
                orden.getCodigo(),
                identificacionPaciente(orden),
                orden.getTipoTrabajo().getNombre(),
                orden.getTipoOrden().getNombre(),
                orden.getEstado().getNombre(),
                orden.getDescripcion(),
                orden.getFechaIngreso(),
                orden.getFechaEstimadaEntrega(),
                orden.getPrecioBase(),
                orden.getRecargoUrgencia(),
                orden.getPrecioTotal());
    }
}
