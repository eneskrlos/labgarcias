package com.labgarcias.ordenes.service;

import java.time.OffsetDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.catalogos.domain.Estado;
import com.labgarcias.catalogos.service.EstadoService;
import com.labgarcias.ordenes.domain.EstadoOrdenCambiadoEvent;
import com.labgarcias.ordenes.domain.Orden;
import com.labgarcias.ordenes.domain.OrdenHistorialEstado;
import com.labgarcias.ordenes.dto.OrdenDetalleResponse;
import com.labgarcias.ordenes.repository.OrdenHistorialEstadoRepository;
import com.labgarcias.ordenes.repository.OrdenRepository;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.shared.excepcion.ConflictoException;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;

import jakarta.persistence.EntityManager;

/**
 * CU-06/CU-20: ciclo de vida de la orden. Va aparte de OrdenService porque juntarlos
 * dejaría una clase de más de 250 líneas (Agente.md 6.2).
 *
 * RN-04: las transiciones válidas NO están codificadas acá. Se deducen de la tabla
 * `estado`: `orden_secuencia` define el orden del flujo y `es_terminal` marca dónde
 * termina. spec.md §4.2 prohíbe crear estados y modificar esas dos columnas
 * justamente porque el flujo lineal depende de ellas, así que la tabla es la única
 * fuente de verdad y este servicio se limita a leerla.
 */
@Service
public class OrdenEstadoService {

    /** RN-17/CU-20: único estado fuera del flujo productivo; se alcanza solo cancelando. */
    private static final String CODIGO_CANCELADO = "CANCELADO";

    private static final String CODIGO_TRANSICION_NO_PERMITIDA = "TRANSICION_NO_PERMITIDA";

    private final OrdenRepository ordenRepository;
    private final OrdenHistorialEstadoRepository historialRepository;
    private final EstadoService estadoService;
    private final OrdenService ordenService;
    private final ApplicationEventPublisher eventos;
    private final EntityManager entityManager;

    public OrdenEstadoService(OrdenRepository ordenRepository,
                              OrdenHistorialEstadoRepository historialRepository,
                              EstadoService estadoService,
                              OrdenService ordenService,
                              ApplicationEventPublisher eventos,
                              EntityManager entityManager) {
        this.ordenRepository = ordenRepository;
        this.historialRepository = historialRepository;
        this.estadoService = estadoService;
        this.ordenService = ordenService;
        this.eventos = eventos;
        this.entityManager = entityManager;
    }

    /**
     * CU-06/§5.5: avanza la orden al estado inmediatamente siguiente del flujo.
     * Todo ocurre en la misma transacción: si algo falla, no queda ni el cambio de
     * estado, ni el registro de historial, ni el evento publicado (§5.5 criterio 4).
     */
    @Transactional
    public OrdenDetalleResponse avanzarEstado(Long ordenId, String estadoCodigo, Long autorId) {
        Orden orden = ordenRepository.findById(ordenId).orElseThrow(this::ordenNoEncontrada);
        Estado destino = estadoService.obtenerPorCodigoParaOrden(estadoCodigo);
        validarAvance(orden.getEstado(), destino);

        orden.setEstado(destino);
        historialRepository.saveAndFlush(registroDe(orden, destino, autorId));
        eventos.publishEvent(new EstadoOrdenCambiadoEvent(
                orden.getId(), orden.getCodigo(), orden.getOdontologo().getId(),
                orden.getPacienteCodigo(), destino.getNombre()));

        return ordenService.obtenerDetalle(ordenId, autorId, true);
    }

    /**
     * CU-20/§5.6: el odontólogo cancela su propia orden.
     * S-08 sigue sin resolver, así que la cancelación no publica evento: no notifica.
     */
    @Transactional
    public OrdenDetalleResponse cancelar(Long ordenId, Long odontologoId) {
        Orden orden = ordenRepository.findById(ordenId).orElseThrow(this::ordenNoEncontrada);
        // RN-01: una orden ajena responde 404, no 403, para no revelar que existe.
        if (!orden.getOdontologo().getId().equals(odontologoId)) {
            throw ordenNoEncontrada();
        }
        if (orden.getEstado().isEsTerminal()) {
            throw new ConflictoException("ORDEN_NO_CANCELABLE",
                    "Una orden en " + orden.getEstado().getNombre() + " ya no puede cancelarse.");
        }

        Estado cancelado = estadoService.obtenerPorCodigoParaOrden(CODIGO_CANCELADO);
        orden.setEstado(cancelado);
        orden.setFechaCancelacion(OffsetDateTime.now());
        // P-14 sin resolver: cargo_cancelacion queda en null a propósito. No calcular nada.
        historialRepository.saveAndFlush(registroDe(orden, cancelado, odontologoId));

        return ordenService.obtenerDetalle(ordenId, odontologoId, false);
    }

    /**
     * RN-04: solo se avanza al estado con la secuencia inmediatamente superior.
     * Los retrocesos quedan cubiertos por la misma comparación: una secuencia menor
     * tampoco es "la siguiente" (P-02 sin resolver, no hay marcha atrás).
     */
    private void validarAvance(Estado actual, Estado destino) {
        if (actual.isEsTerminal()) {
            throw transicionNoPermitida(
                    "La orden está en " + actual.getNombre() + " y ya no admite cambios de estado.");
        }
        if (destino.getOrdenSecuencia() == null) {
            // Solo CANCELADO está fuera del flujo. RN-17/CU-20: cancelar es del odontólogo.
            throw transicionNoPermitida("El laboratorio no puede cancelar una orden.");
        }
        if (destino.getOrdenSecuencia() != actual.getOrdenSecuencia() + 1) {
            throw transicionNoPermitida("Desde " + actual.getNombre()
                    + " solo se puede avanzar a la etapa inmediatamente siguiente.");
        }
    }

    private OrdenHistorialEstado registroDe(Orden orden, Estado estado, Long autorId) {
        OrdenHistorialEstado registro = new OrdenHistorialEstado();
        registro.setOrden(orden);
        registro.setEstado(estado);
        registro.setUsuario(entityManager.getReference(Usuario.class, autorId));
        return registro;
    }

    private ConflictoException transicionNoPermitida(String mensaje) {
        return new ConflictoException(CODIGO_TRANSICION_NO_PERMITIDA, mensaje, "estadoCodigo");
    }

    private RecursoNoEncontradoException ordenNoEncontrada() {
        return new RecursoNoEncontradoException("ORDEN_NO_ENCONTRADA", "No existe la orden solicitada.");
    }
}
