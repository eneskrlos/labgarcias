package com.labgarcias.catalogos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.labgarcias.catalogos.domain.Estado;
import com.labgarcias.catalogos.dto.EstadoActualizarRequest;
import com.labgarcias.catalogos.dto.EstadoResponse;
import com.labgarcias.catalogos.repository.EstadoRepository;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;

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

    @Test
    void actualizarConIdInexistenteLanza404() {
        when(estadoRepository.findById((short) 99)).thenReturn(Optional.empty());
        EstadoActualizarRequest request = new EstadoActualizarRequest("Nuevo nombre", "Nueva descripción");

        assertThatThrownBy(() -> estadoService.actualizar((short) 99, request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getCodigo()).isEqualTo("ESTADO_NO_ENCONTRADO"));
    }

    @Test
    void actualizarSoloCambiaNombreYDescripcion() {
        Estado estado = new Estado();
        estado.setCodigo("EN_PRODUCCION");
        estado.setNombre("En producción");
        estado.setDescripcion("Descripción vieja");
        estado.setOrdenSecuencia((short) 3);
        estado.setEsTerminal(false);
        estado.setEsProductivo(true);
        estado.setActivo(true);
        when(estadoRepository.findById((short) 3)).thenReturn(Optional.of(estado));

        EstadoActualizarRequest request = new EstadoActualizarRequest("En fabricación", "Descripción nueva");
        EstadoResponse respuesta = estadoService.actualizar((short) 3, request);

        assertThat(respuesta.nombre()).isEqualTo("En fabricación");
        assertThat(respuesta.descripcion()).isEqualTo("Descripción nueva");
        // RN-04: estos campos no los toca este endpoint, quedan tal cual estaban.
        assertThat(respuesta.codigo()).isEqualTo("EN_PRODUCCION");
        assertThat(respuesta.ordenSecuencia()).isEqualTo((short) 3);
        assertThat(respuesta.esTerminal()).isFalse();
        assertThat(respuesta.esProductivo()).isTrue();
    }
}
