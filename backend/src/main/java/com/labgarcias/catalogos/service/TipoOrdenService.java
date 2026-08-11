package com.labgarcias.catalogos.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.catalogos.domain.TipoOrden;
import com.labgarcias.catalogos.dto.TipoOrdenResponse;
import com.labgarcias.catalogos.repository.TipoOrdenRepository;

@Service
public class TipoOrdenService {

    private final TipoOrdenRepository tipoOrdenRepository;

    public TipoOrdenService(TipoOrdenRepository tipoOrdenRepository) {
        this.tipoOrdenRepository = tipoOrdenRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoOrdenResponse> listar() {
        return tipoOrdenRepository.findAll().stream()
                .map(this::aRespuesta)
                .toList();
    }

    private TipoOrdenResponse aRespuesta(TipoOrden tipoOrden) {
        return new TipoOrdenResponse(tipoOrden.getCodigo().name(), tipoOrden.getNombre(), tipoOrden.getRecargoMonto());
    }
}
