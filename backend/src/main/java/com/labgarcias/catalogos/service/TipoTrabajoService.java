package com.labgarcias.catalogos.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.catalogos.domain.TipoTrabajo;
import com.labgarcias.catalogos.dto.TipoTrabajoResponse;
import com.labgarcias.catalogos.repository.TipoTrabajoRepository;

@Service
public class TipoTrabajoService {

    private final TipoTrabajoRepository tipoTrabajoRepository;

    public TipoTrabajoService(TipoTrabajoRepository tipoTrabajoRepository) {
        this.tipoTrabajoRepository = tipoTrabajoRepository;
    }

    /** CU-16: el odontólogo solo ve tipos activos. */
    @Transactional(readOnly = true)
    public List<TipoTrabajoResponse> listarActivos() {
        return tipoTrabajoRepository.findAllByActivoTrueOrderByNombreAsc().stream()
                .map(this::aRespuesta)
                .toList();
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
