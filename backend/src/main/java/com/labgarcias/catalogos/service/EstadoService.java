package com.labgarcias.catalogos.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.catalogos.domain.Estado;
import com.labgarcias.catalogos.dto.EstadoActualizarRequest;
import com.labgarcias.catalogos.dto.EstadoResponse;
import com.labgarcias.catalogos.repository.EstadoRepository;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;
import com.labgarcias.shared.excepcion.ReglaNegocioException;

@Service
public class EstadoService {

    private final EstadoRepository estadoRepository;

    public EstadoService(EstadoRepository estadoRepository) {
        this.estadoRepository = estadoRepository;
    }

    @Transactional(readOnly = true)
    public List<EstadoResponse> listar() {
        return estadoRepository.findAllByOrderByOrdenSecuenciaAsc().stream()
                .map(this::aRespuesta)
                .toList();
    }

    /**
     * CU-06/CU-20: el módulo de órdenes necesita la entidad para leer orden_secuencia y
     * es_terminal, que son los que deciden si una transición es válida (RN-04).
     * Devuelve dominio (no DTO) porque Agente.md 5.4 prohíbe importar el dto de otro módulo.
     */
    @Transactional(readOnly = true)
    public Estado obtenerPorCodigoParaOrden(String codigo) {
        return estadoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ReglaNegocioException("ESTADO_NO_ENCONTRADO",
                        "No existe un estado con ese código.", "estadoCodigo"));
    }

    /**
     * RN-04/§5.5: la etapa a la que se puede avanzar desde `actual`, o vacío si no hay ninguna.
     *
     * Es la misma regla que aplica `OrdenEstadoService` al validar una transición, leída del
     * mismo lugar: `orden_secuencia` de la tabla. Vive acá porque el flujo es del catálogo, y
     * porque así el módulo de órdenes puede **exponerla** en el detalle sin duplicarla (§8: las
     * transiciones vienen del backend, el cliente no las calcula).
     *
     * Devuelve vacío desde un estado terminal —`ENTREGADO` y `CANCELADO`— y desde cualquiera que
     * esté fuera del flujo, que es lo que hace que la pantalla no ofrezca un botón imposible.
     */
    @Transactional(readOnly = true)
    public Optional<Estado> siguienteEnFlujo(Estado actual) {
        if (actual.isEsTerminal() || actual.getOrdenSecuencia() == null) {
            return Optional.empty();
        }
        return estadoRepository.findByOrdenSecuencia((short) (actual.getOrdenSecuencia() + 1));
    }

    /** CU-22/RN-04: solo nombre y descripcion son editables; codigo/orden_secuencia/es_terminal/es_productivo quedan fijos. */
    @Transactional
    public EstadoResponse actualizar(Short id, EstadoActualizarRequest request) {
        Estado estado = estadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("ESTADO_NO_ENCONTRADO", "No existe el estado solicitado."));
        estado.setNombre(request.nombre());
        estado.setDescripcion(request.descripcion());
        return aRespuesta(estado);
    }

    private EstadoResponse aRespuesta(Estado estado) {
        return new EstadoResponse(
                estado.getId(),
                estado.getCodigo(),
                estado.getNombre(),
                estado.getDescripcion(),
                estado.getOrdenSecuencia(),
                estado.isEsTerminal(),
                estado.isEsProductivo(),
                estado.isActivo());
    }
}
