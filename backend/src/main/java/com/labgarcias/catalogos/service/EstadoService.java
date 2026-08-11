package com.labgarcias.catalogos.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.catalogos.domain.Estado;
import com.labgarcias.catalogos.dto.EstadoResponse;
import com.labgarcias.catalogos.repository.EstadoRepository;

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
