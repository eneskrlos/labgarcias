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

import com.labgarcias.catalogos.domain.TipoTrabajo;
import com.labgarcias.catalogos.dto.TipoTrabajoResponse;
import com.labgarcias.catalogos.repository.TipoTrabajoRepository;

@ExtendWith(MockitoExtension.class)
class TipoTrabajoServiceTest {

    @Mock
    private TipoTrabajoRepository tipoTrabajoRepository;

    @InjectMocks
    private TipoTrabajoService tipoTrabajoService;

    @Test
    void listarActivosUsaElRepositorioFiltradoYMapeaLosCampos() {
        TipoTrabajo tipoTrabajo = new TipoTrabajo();
        tipoTrabajo.setNombre("PLACA ACTIVA");
        tipoTrabajo.setDiasEstimados((short) 7);
        tipoTrabajo.setPrecio(new BigDecimal("250.00"));
        tipoTrabajo.setActivo(true);
        when(tipoTrabajoRepository.findAllByActivoTrueOrderByNombreAsc()).thenReturn(List.of(tipoTrabajo));

        List<TipoTrabajoResponse> resultado = tipoTrabajoService.listarActivos();

        assertThat(resultado).hasSize(1);
        TipoTrabajoResponse respuesta = resultado.get(0);
        assertThat(respuesta.nombre()).isEqualTo("PLACA ACTIVA");
        assertThat(respuesta.diasEstimados()).isEqualTo(7);
        assertThat(respuesta.precio()).isEqualByComparingTo("250.00");
        assertThat(respuesta.activo()).isTrue();
    }

    @Test
    void sinTiposActivosDevuelveListaVacia() {
        when(tipoTrabajoRepository.findAllByActivoTrueOrderByNombreAsc()).thenReturn(List.of());

        assertThat(tipoTrabajoService.listarActivos()).isEmpty();
    }
}
