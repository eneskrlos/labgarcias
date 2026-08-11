package com.labgarcias.catalogos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.labgarcias.catalogos.domain.Estado;
import com.labgarcias.catalogos.dto.EstadoResponse;
import com.labgarcias.catalogos.repository.EstadoRepository;

@ExtendWith(MockitoExtension.class)
class EstadoServiceTest {

    @Mock
    private EstadoRepository estadoRepository;

    @InjectMocks
    private EstadoService estadoService;

    @Test
    void listarMapeaTodosLosCamposIncluidoElOrdenNuloDeCancelado() {
        Estado recibido = new Estado();
        recibido.setCodigo("RECIBIDO");
        recibido.setNombre("Recibido");
        recibido.setOrdenSecuencia((short) 1);
        recibido.setEsTerminal(false);
        recibido.setEsProductivo(true);
        recibido.setActivo(true);

        Estado cancelado = new Estado();
        cancelado.setCodigo("CANCELADO");
        cancelado.setNombre("Cancelado");
        cancelado.setOrdenSecuencia(null);
        cancelado.setEsTerminal(true);
        cancelado.setEsProductivo(false);
        cancelado.setActivo(true);

        when(estadoRepository.findAllByOrderByOrdenSecuenciaAsc()).thenReturn(List.of(recibido, cancelado));

        List<EstadoResponse> resultado = estadoService.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).codigo()).isEqualTo("RECIBIDO");
        assertThat(resultado.get(0).ordenSecuencia()).isEqualTo((short) 1);
        assertThat(resultado.get(0).esProductivo()).isTrue();
        assertThat(resultado.get(1).codigo()).isEqualTo("CANCELADO");
        assertThat(resultado.get(1).ordenSecuencia()).isNull();
        assertThat(resultado.get(1).esTerminal()).isTrue();
        assertThat(resultado.get(1).esProductivo()).isFalse();
    }
}
