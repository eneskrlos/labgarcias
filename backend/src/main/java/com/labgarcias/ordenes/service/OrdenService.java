package com.labgarcias.ordenes.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.catalogos.domain.CodigoTipoOrden;
import com.labgarcias.catalogos.domain.TipoOrden;
import com.labgarcias.catalogos.domain.TipoTrabajo;
import com.labgarcias.catalogos.service.TipoOrdenService;
import com.labgarcias.catalogos.service.TipoTrabajoService;
import com.labgarcias.ordenes.domain.Orden;
import com.labgarcias.ordenes.domain.OrdenCreadaEvent;
import com.labgarcias.ordenes.dto.CrearOrdenRequest;
import com.labgarcias.ordenes.dto.OrdenResponse;
import com.labgarcias.ordenes.repository.OrdenHistorialEstadoRepository;
import com.labgarcias.ordenes.repository.OrdenRepository;
import com.labgarcias.seguridad.domain.Usuario;

import jakarta.persistence.EntityManager;

/** CU-09: creación de órdenes. Los valores derivados los calcula el backend, nunca el cliente. */
@Service
public class OrdenService {

    private final OrdenRepository ordenRepository;
    private final OrdenHistorialEstadoRepository historialRepository;
    private final TipoTrabajoService tipoTrabajoService;
    private final TipoOrdenService tipoOrdenService;
    private final FabricaOrden fabricaOrden;
    private final ApplicationEventPublisher eventos;
    private final EntityManager entityManager;

    public OrdenService(OrdenRepository ordenRepository,
                        OrdenHistorialEstadoRepository historialRepository,
                        TipoTrabajoService tipoTrabajoService,
                        TipoOrdenService tipoOrdenService,
                        FabricaOrden fabricaOrden,
                        ApplicationEventPublisher eventos,
                        EntityManager entityManager) {
        this.ordenRepository = ordenRepository;
        this.historialRepository = historialRepository;
        this.tipoTrabajoService = tipoTrabajoService;
        this.tipoOrdenService = tipoOrdenService;
        this.fabricaOrden = fabricaOrden;
        this.eventos = eventos;
        this.entityManager = entityManager;
    }

    @Transactional
    public OrdenResponse crear(CrearOrdenRequest request, Long odontologoId) {
        TipoTrabajo tipoTrabajo = tipoTrabajoService.obtenerActivoParaOrden(request.tipoTrabajoId());
        TipoOrden tipoOrden = tipoOrdenService.obtenerPorCodigo(CodigoTipoOrden.valueOf(request.tipoOrdenCodigo()));
        Usuario odontologo = entityManager.getReference(Usuario.class, odontologoId);

        Orden orden = fabricaOrden.crear(request, odontologo, tipoTrabajo, tipoOrden);
        // saveAndFlush: codigo, paciente_codigo y precio_total los genera la base al insertar.
        Orden persistida = ordenRepository.saveAndFlush(orden);
        historialRepository.save(fabricaOrden.registroInicialDe(persistida));

        eventos.publishEvent(new OrdenCreadaEvent(
                persistida.getId(), persistida.getCodigo(), persistida.getPacienteCodigo(),
                tipoOrden.isNotificaAdmin()));

        return aRespuesta(persistida);
    }

    /** RN-03/RN-22: la respuesta se arma con iniciales + código, nunca con el nombre del paciente. */
    private OrdenResponse aRespuesta(Orden orden) {
        return new OrdenResponse(
                orden.getId(),
                orden.getCodigo(),
                orden.getPacienteIniciales() + " - Caso #" + orden.getPacienteCodigo(),
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
