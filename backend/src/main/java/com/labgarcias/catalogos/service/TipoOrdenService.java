package com.labgarcias.catalogos.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.catalogos.domain.CodigoTipoOrden;
import com.labgarcias.catalogos.domain.TipoOrden;
import com.labgarcias.catalogos.dto.TipoOrdenResponse;
import com.labgarcias.catalogos.repository.TipoOrdenRepository;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;

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

    /**
     * RN-11/CU-09: el comportamiento diferencial NORMAL/URGENTE es dato de esta tabla.
     * Devuelve dominio (no DTO) porque Agente.md 5.4 prohíbe importar el dto de otro módulo.
     */
    @Transactional(readOnly = true)
    public TipoOrden obtenerPorCodigo(CodigoTipoOrden codigo) {
        return tipoOrdenRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("TIPO_ORDEN_NO_ENCONTRADO",
                        "No existe el tipo de orden solicitado."));
    }

    private TipoOrdenResponse aRespuesta(TipoOrden tipoOrden) {
        return new TipoOrdenResponse(tipoOrden.getCodigo().name(), tipoOrden.getNombre(), tipoOrden.getRecargoMonto());
    }
}
