package com.labgarcias.catalogos.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.catalogos.domain.TipoTrabajo;
import com.labgarcias.catalogos.dto.TipoTrabajoRequest;
import com.labgarcias.catalogos.dto.TipoTrabajoResponse;
import com.labgarcias.catalogos.repository.TipoTrabajoRepository;
import com.labgarcias.shared.dto.PaginaResponse;
import com.labgarcias.shared.excepcion.ConflictoException;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;
import com.labgarcias.shared.excepcion.ReglaNegocioException;
import com.labgarcias.shared.util.ConstantesDominio;
import com.labgarcias.shared.util.ValidadorPaginacion;

@Service
public class TipoTrabajoService {

    private final TipoTrabajoRepository tipoTrabajoRepository;

    public TipoTrabajoService(TipoTrabajoRepository tipoTrabajoRepository) {
        this.tipoTrabajoRepository = tipoTrabajoRepository;
    }

    /** CU-16/CU-09: catálogo completo sin paginar, para el selector de nueva orden. */
    @Transactional(readOnly = true)
    public List<TipoTrabajoResponse> listarActivos() {
        return tipoTrabajoRepository.findAllByActivoTrueOrderByNombreAsc().stream()
                .map(this::aRespuesta)
                .toList();
    }

    /** CU-16: listado paginado de administración, con filtros opcionales activo/busqueda. */
    @Transactional(readOnly = true)
    public PaginaResponse<TipoTrabajoResponse> listarPaginado(Pageable pageable, Boolean activo, String busqueda) {
        ValidadorPaginacion.validarTamano(pageable.getPageSize());
        Page<TipoTrabajoResponse> pagina = tipoTrabajoRepository.buscar(activo, busqueda, pageable)
                .map(this::aRespuesta);
        return PaginaResponse.de(pagina);
    }

    /** Alimenta la precarga del formulario de edición (spec.md §8.1). */
    @Transactional(readOnly = true)
    public TipoTrabajoResponse obtenerPorId(Integer id) {
        return aRespuesta(buscarPorId(id));
    }

    /**
     * CU-09: el módulo de órdenes necesita la entidad para tomar la foto de precio y días.
     * Devuelve dominio (no DTO) porque Agente.md 5.4 prohíbe importar el dto de otro módulo.
     */
    @Transactional(readOnly = true)
    public TipoTrabajo obtenerActivoParaOrden(Integer id) {
        TipoTrabajo tipoTrabajo = tipoTrabajoRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("TIPO_TRABAJO_INACTIVO",
                        "El tipo de trabajo no existe o no está disponible.", "tipoTrabajoId"));
        if (!tipoTrabajo.isActivo()) {
            throw new ReglaNegocioException("TIPO_TRABAJO_INACTIVO",
                    "El tipo de trabajo no existe o no está disponible.", "tipoTrabajoId");
        }
        return tipoTrabajo;
    }

    @Transactional
    public TipoTrabajoResponse crear(TipoTrabajoRequest request) {
        validarDiasYPrecio(request);
        validarNombreDisponible(request.nombre(), null);

        TipoTrabajo tipoTrabajo = new TipoTrabajo();
        tipoTrabajo.setNombre(request.nombre());
        tipoTrabajo.setDiasEstimados(request.diasEstimados().shortValue());
        tipoTrabajo.setPrecio(request.precio());
        tipoTrabajo.setActivo(true);
        return aRespuesta(tipoTrabajoRepository.save(tipoTrabajo));
    }

    @Transactional
    public TipoTrabajoResponse actualizar(Integer id, TipoTrabajoRequest request) {
        validarDiasYPrecio(request);
        validarNombreDisponible(request.nombre(), id);

        TipoTrabajo tipoTrabajo = buscarPorId(id);
        tipoTrabajo.setNombre(request.nombre());
        tipoTrabajo.setDiasEstimados(request.diasEstimados().shortValue());
        tipoTrabajo.setPrecio(request.precio());
        return aRespuesta(tipoTrabajo);
    }

    /** CU-16 A1: activa/desactiva sin borrar el registro (no hay endpoint de eliminación). */
    @Transactional
    public TipoTrabajoResponse cambiarEstado(Integer id, boolean activo) {
        TipoTrabajo tipoTrabajo = buscarPorId(id);
        tipoTrabajo.setActivo(activo);
        return aRespuesta(tipoTrabajo);
    }

    private TipoTrabajo buscarPorId(Integer id) {
        return tipoTrabajoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("TIPO_TRABAJO_NO_ENCONTRADO", "No existe el tipo de trabajo solicitado."));
    }

    private void validarDiasYPrecio(TipoTrabajoRequest request) {
        if (request.diasEstimados() < ConstantesDominio.DIAS_HABILES_MINIMOS) {
            throw new ReglaNegocioException("DIAS_ESTIMADOS_INSUFICIENTES",
                    "Los días estimados deben ser al menos " + ConstantesDominio.DIAS_HABILES_MINIMOS + ".", "diasEstimados");
        }
        if (request.precio().compareTo(ConstantesDominio.PRECIO_MINIMO_TRABAJO) < 0) {
            throw new ReglaNegocioException("PRECIO_INSUFICIENTE",
                    "El precio debe ser al menos " + ConstantesDominio.PRECIO_MINIMO_TRABAJO + ".", "precio");
        }
    }

    private void validarNombreDisponible(String nombre, Integer idActual) {
        Optional<TipoTrabajo> existente = tipoTrabajoRepository.findByNombre(nombre);
        if (existente.isPresent() && !existente.get().getId().equals(idActual)) {
            throw new ConflictoException("TIPO_TRABAJO_DUPLICADO", "Ya existe un tipo de trabajo con ese nombre.", "nombre");
        }
    }

    private TipoTrabajoResponse aRespuesta(TipoTrabajo tipoTrabajo) {
        return new TipoTrabajoResponse(
                tipoTrabajo.getId(),
                tipoTrabajo.getNombre(),
                tipoTrabajo.getDiasEstimados().intValue(),
                tipoTrabajo.getPrecio(),
                tipoTrabajo.isActivo());
    }
}
