package com.labgarcias.catalogos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.labgarcias.catalogos.domain.CodigoTipoOrden;
import com.labgarcias.catalogos.domain.TipoOrden;
import com.labgarcias.catalogos.dto.TipoOrdenResponse;
import com.labgarcias.catalogos.repository.TipoOrdenRepository;

@ExtendWith(MockitoExtension.class)
class TipoOrdenServiceTest {

    @Mock
    private TipoOrdenRepository tipoOrdenRepository;

    @InjectMocks
    private TipoOrdenService tipoOrdenService;

    @Test
    void listarDevuelveSoloCodigoNombreYRecargoSegunSpec() {
        TipoOrden urgente = new TipoOrden();
        urgente.setCodigo(CodigoTipoOrden.URGENTE);
        urgente.setNombre("Urgente");
        urgente.setRecargoMonto(new BigDecimal("200.00"));
        when(tipoOrdenRepository.findAll()).thenReturn(List.of(urgente));

        List<TipoOrdenResponse> resultado = tipoOrdenService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).codigo()).isEqualTo("URGENTE");
        assertThat(resultado.get(0).nombre()).isEqualTo("Urgente");
        assertThat(resultado.get(0).recargoMonto()).isEqualByComparingTo("200.00");
    }
}
