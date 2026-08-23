package com.labgarcias.catalogos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.labgarcias.catalogos.domain.TipoTrabajo;
import com.labgarcias.catalogos.dto.TipoTrabajoRequest;
import com.labgarcias.catalogos.dto.TipoTrabajoResponse;
import com.labgarcias.catalogos.repository.TipoTrabajoRepository;
import com.labgarcias.shared.dto.PaginaResponse;
import com.labgarcias.shared.excepcion.ConflictoException;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;
import com.labgarcias.shared.excepcion.ReglaNegocioException;
import com.labgarcias.shared.excepcion.ValidacionException;

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

    @Test
    void listarPaginadoConTamanoInvalidoEsRechazado() {
        Pageable pageable = PageRequest.of(0, 15);

        assertThatThrownBy(() -> tipoTrabajoService.listarPaginado(pageable, null, null))
                .isInstanceOf(ValidacionException.class)
                .satisfies(ex -> assertThat(((ValidacionException) ex).getCodigo()).isEqualTo("TAMANO_PAGINA_INVALIDO"));
    }

    @Test
    void listarPaginadoConTamanoValidoDelegaEnElRepositorioYMapeaLaPagina() {
        TipoTrabajo tipoTrabajo = new TipoTrabajo();
        tipoTrabajo.setNombre("VIEJO");
        tipoTrabajo.setDiasEstimados((short) 7);
        tipoTrabajo.setPrecio(new BigDecimal("250.00"));
        tipoTrabajo.setActivo(false);
        Pageable pageable = PageRequest.of(0, 10);
        when(tipoTrabajoRepository.buscar(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(tipoTrabajo), pageable, 45));

        PaginaResponse<TipoTrabajoResponse> respuesta = tipoTrabajoService.listarPaginado(pageable, null, null);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().get(0).activo()).isFalse();
        assertThat(respuesta.total()).isEqualTo(45);
        assertThat(respuesta.pagina()).isEqualTo(0);
        assertThat(respuesta.tamano()).isEqualTo(10);
        assertThat(respuesta.totalPaginas()).isEqualTo(5);
    }

    @Test
    void listarPaginadoPropagaLosFiltrosAlRepositorio() {
        Pageable pageable = PageRequest.of(0, 10);
        when(tipoTrabajoRepository.buscar(true, "placa", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        tipoTrabajoService.listarPaginado(pageable, true, "placa");

        verify(tipoTrabajoRepository).buscar(true, "placa", pageable);
    }

    @Test
    void obtenerPorIdConIdInexistenteLanza404() {
        when(tipoTrabajoRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoTrabajoService.obtenerPorId(99))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getCodigo()).isEqualTo("TIPO_TRABAJO_NO_ENCONTRADO"));
    }

    @Test
    void obtenerPorIdDevuelveElTipoMapeado() {
        TipoTrabajo tipoTrabajo = new TipoTrabajo();
        tipoTrabajo.setNombre("PLACA ACTIVA");
        tipoTrabajo.setDiasEstimados((short) 7);
        tipoTrabajo.setPrecio(new BigDecimal("250.00"));
        tipoTrabajo.setActivo(true);
        when(tipoTrabajoRepository.findById(16)).thenReturn(Optional.of(tipoTrabajo));

        TipoTrabajoResponse respuesta = tipoTrabajoService.obtenerPorId(16);

        assertThat(respuesta.nombre()).isEqualTo("PLACA ACTIVA");
    }

    @Test
    void obtenerActivoParaOrdenDevuelveLaEntidadCuandoElTipoEstaActivo() {
        TipoTrabajo activo = new TipoTrabajo();
        activo.setNombre("PLACA ACTIVA");
        activo.setDiasEstimados((short) 7);
        activo.setPrecio(new BigDecimal("250.00"));
        activo.setActivo(true);
        when(tipoTrabajoRepository.findById(16)).thenReturn(Optional.of(activo));

        assertThat(tipoTrabajoService.obtenerActivoParaOrden(16)).isSameAs(activo);
    }

    @Test
    void obtenerActivoParaOrdenRechazaUnTipoInactivo() {
        TipoTrabajo inactivo = new TipoTrabajo();
        inactivo.setNombre("VIEJO");
        inactivo.setActivo(false);
        when(tipoTrabajoRepository.findById(16)).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> tipoTrabajoService.obtenerActivoParaOrden(16))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo()).isEqualTo("TIPO_TRABAJO_INACTIVO"));
    }

    @Test
    void obtenerActivoParaOrdenRechazaUnTipoInexistenteConElMismoCodigo() {
        // No se distingue "no existe" de "inactivo": desde CU-09 el resultado es el mismo.
        when(tipoTrabajoRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoTrabajoService.obtenerActivoParaOrden(99))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo()).isEqualTo("TIPO_TRABAJO_INACTIVO"));
    }

    @Test
    void crearConMenosDe7DiasEsRechazado() {
        TipoTrabajoRequest request = new TipoTrabajoRequest("NUEVO", 6, new BigDecimal("250.00"));

        assertThatThrownBy(() -> tipoTrabajoService.crear(request))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo()).isEqualTo("DIAS_ESTIMADOS_INSUFICIENTES"));

        verify(tipoTrabajoRepository, never()).save(any());
    }

    @Test
    void crearConPrecioMenorA250EsRechazado() {
        TipoTrabajoRequest request = new TipoTrabajoRequest("NUEVO", 7, new BigDecimal("249.00"));

        assertThatThrownBy(() -> tipoTrabajoService.crear(request))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo()).isEqualTo("PRECIO_INSUFICIENTE"));

        verify(tipoTrabajoRepository, never()).save(any());
    }

    @Test
    void crearConNombreYaExistenteEsRechazado() {
        TipoTrabajoRequest request = new TipoTrabajoRequest("PLACA ACTIVA", 7, new BigDecimal("250.00"));
        // Un registro ya persistido siempre tiene id (autogenerado); se mockea para simularlo.
        TipoTrabajo existente = mock(TipoTrabajo.class);
        when(existente.getId()).thenReturn(1);
        when(tipoTrabajoRepository.findByNombre("PLACA ACTIVA")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> tipoTrabajoService.crear(request))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigo()).isEqualTo("TIPO_TRABAJO_DUPLICADO"));

        verify(tipoTrabajoRepository, never()).save(any());
    }

    @Test
    void crearValidoQuedaActivoPorDefecto() {
        TipoTrabajoRequest request = new TipoTrabajoRequest("NUEVO", 7, new BigDecimal("250.00"));
        when(tipoTrabajoRepository.findByNombre("NUEVO")).thenReturn(Optional.empty());
        when(tipoTrabajoRepository.save(any(TipoTrabajo.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoTrabajoResponse respuesta = tipoTrabajoService.crear(request);

        assertThat(respuesta.activo()).isTrue();
        assertThat(respuesta.nombre()).isEqualTo("NUEVO");
    }

    @Test
    void actualizarConIdInexistenteLanza404() {
        TipoTrabajoRequest request = new TipoTrabajoRequest("NUEVO", 7, new BigDecimal("250.00"));
        when(tipoTrabajoRepository.findByNombre("NUEVO")).thenReturn(Optional.empty());
        when(tipoTrabajoRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoTrabajoService.actualizar(99, request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getCodigo()).isEqualTo("TIPO_TRABAJO_NO_ENCONTRADO"));
    }

    @Test
    void actualizarNoSeRechazaASiMismoPorNombreDuplicado() {
        // TipoTrabajo.id lo asigna @GeneratedValue (sin setter): se mockea para poder
        // simular que findByNombre() encuentra el MISMO registro que se está editando.
        TipoTrabajo actual = mock(TipoTrabajo.class);
        when(actual.getId()).thenReturn(1);
        when(actual.getNombre()).thenReturn("PLACA ACTIVA");
        when(actual.getDiasEstimados()).thenReturn((short) 10);
        when(actual.getPrecio()).thenReturn(new BigDecimal("300.00"));
        when(tipoTrabajoRepository.findByNombre("PLACA ACTIVA")).thenReturn(Optional.of(actual));
        when(tipoTrabajoRepository.findById(1)).thenReturn(Optional.of(actual));

        TipoTrabajoRequest request = new TipoTrabajoRequest("PLACA ACTIVA", 10, new BigDecimal("300.00"));

        TipoTrabajoResponse respuesta = tipoTrabajoService.actualizar(1, request);

        assertThat(respuesta.nombre()).isEqualTo("PLACA ACTIVA");
        verify(actual).setNombre("PLACA ACTIVA");
    }

    @Test
    void actualizarRechazaElNombreDeOtroRegistroDistinto() {
        TipoTrabajo otro = mock(TipoTrabajo.class);
        when(otro.getId()).thenReturn(2);
        when(tipoTrabajoRepository.findByNombre("PLACA ACTIVA")).thenReturn(Optional.of(otro));

        TipoTrabajoRequest request = new TipoTrabajoRequest("PLACA ACTIVA", 10, new BigDecimal("300.00"));

        assertThatThrownBy(() -> tipoTrabajoService.actualizar(1, request))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigo()).isEqualTo("TIPO_TRABAJO_DUPLICADO"));
    }

    @Test
    void actualizarValidoModificaLosCampos() {
        TipoTrabajo existente = new TipoTrabajo();
        existente.setNombre("VIEJO NOMBRE");
        existente.setDiasEstimados((short) 7);
        existente.setPrecio(new BigDecimal("250.00"));
        existente.setActivo(true);
        when(tipoTrabajoRepository.findByNombre("NUEVO NOMBRE")).thenReturn(Optional.empty());
        when(tipoTrabajoRepository.findById(5)).thenReturn(Optional.of(existente));

        TipoTrabajoRequest request = new TipoTrabajoRequest("NUEVO NOMBRE", 10, new BigDecimal("300.00"));
        TipoTrabajoResponse respuesta = tipoTrabajoService.actualizar(5, request);

        assertThat(respuesta.nombre()).isEqualTo("NUEVO NOMBRE");
        assertThat(respuesta.diasEstimados()).isEqualTo(10);
        assertThat(respuesta.precio()).isEqualByComparingTo("300.00");
    }

    @Test
    void cambiarEstadoConIdInexistenteLanza404() {
        when(tipoTrabajoRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoTrabajoService.cambiarEstado(99, false))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void cambiarEstadoDesactivaSinBorrarElRegistro() {
        TipoTrabajo existente = new TipoTrabajo();
        existente.setNombre("EN USO");
        existente.setDiasEstimados((short) 7);
        existente.setPrecio(new BigDecimal("250.00"));
        existente.setActivo(true);
        when(tipoTrabajoRepository.findById(3)).thenReturn(Optional.of(existente));

        TipoTrabajoResponse respuesta = tipoTrabajoService.cambiarEstado(3, false);

        assertThat(respuesta.activo()).isFalse();
        assertThat(respuesta.nombre()).isEqualTo("EN USO");
        verify(tipoTrabajoRepository, never()).delete(any());
        verify(tipoTrabajoRepository, never()).deleteById(any());
    }
}
