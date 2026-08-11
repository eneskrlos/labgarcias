package com.labgarcias.licencia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.labgarcias.licencia.domain.EstadoLicencia;
import com.labgarcias.licencia.domain.Licencia;
import com.labgarcias.licencia.dto.LicenciaResponse;
import com.labgarcias.licencia.dto.LicenciaVigenteResponse;
import com.labgarcias.licencia.dto.RegistrarLicenciaRequest;
import com.labgarcias.licencia.repository.LicenciaRepository;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.shared.excepcion.ReglaNegocioException;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class LicenciaServiceTest {

    @Mock
    private LicenciaRepository licenciaRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private LicenciaService licenciaService;

    @Test
    void registrarConFechaVencimientoIgualAFechaInicioEsRechazado() {
        LocalDate fecha = LocalDate.of(2026, 1, 1);
        RegistrarLicenciaRequest request = new RegistrarLicenciaRequest(fecha, fecha, "obs");

        assertThatThrownBy(() -> licenciaService.registrar(request, 1L))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo()).isEqualTo("FECHAS_LICENCIA_INVALIDAS"));

        verify(licenciaRepository, never()).save(any());
    }

    @Test
    void registrarConFechaVencimientoAnteriorEsRechazado() {
        RegistrarLicenciaRequest request = new RegistrarLicenciaRequest(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), null);

        assertThatThrownBy(() -> licenciaService.registrar(request, 1L))
                .isInstanceOf(ReglaNegocioException.class);

        verify(licenciaRepository, never()).save(any());
    }

    @Test
    void registrarUnPeriodoValidoQuedaComoActivaConElUsuarioQueLaActivo() {
        RegistrarLicenciaRequest request = new RegistrarLicenciaRequest(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "Renovación anual");

        Usuario superAdmin = mock(Usuario.class);
        when(superAdmin.getNombreCompleto()).thenReturn("Dr. Super Admin");
        when(entityManager.getReference(eq(Usuario.class), eq(9L))).thenReturn(superAdmin);
        when(licenciaRepository.save(any(Licencia.class))).thenAnswer(inv -> inv.getArgument(0));

        LicenciaResponse respuesta = licenciaService.registrar(request, 9L);

        assertThat(respuesta.estado()).isEqualTo("ACTIVA");
        assertThat(respuesta.fechaInicio()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(respuesta.fechaVencimiento()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(respuesta.observacion()).isEqualTo("Renovación anual");
        assertThat(respuesta.activadaPorNombre()).isEqualTo("Dr. Super Admin");
        assertThat(respuesta.fechaRegistro()).isNotNull();
    }

    @Test
    void obtenerVigenteSinPeriodoActivoDevuelveFalse() {
        when(licenciaRepository.findByEstadoAndFechaInicioLessThanEqualAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoDesc(
                eq(EstadoLicencia.ACTIVA), any(), any())).thenReturn(List.of());

        LicenciaVigenteResponse respuesta = licenciaService.obtenerVigente();

        assertThat(respuesta.vigente()).isFalse();
        assertThat(respuesta.licencia()).isNull();
    }

    @Test
    void obtenerVigenteConPeriodoActivoDevuelveTrueConLosDatos() {
        Licencia licencia = new Licencia();
        licencia.setFechaInicio(LocalDate.now().minusDays(10));
        licencia.setFechaVencimiento(LocalDate.now().plusDays(10));
        licencia.setEstado(EstadoLicencia.ACTIVA);
        when(licenciaRepository.findByEstadoAndFechaInicioLessThanEqualAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoDesc(
                eq(EstadoLicencia.ACTIVA), any(), any())).thenReturn(List.of(licencia));

        LicenciaVigenteResponse respuesta = licenciaService.obtenerVigente();

        assertThat(respuesta.vigente()).isTrue();
        assertThat(respuesta.licencia()).isNotNull();
    }

    @Test
    void listarHistoricoMapeaTodosLosPeriodos() {
        Licencia licencia1 = new Licencia();
        licencia1.setEstado(EstadoLicencia.VENCIDA);
        Licencia licencia2 = new Licencia();
        licencia2.setEstado(EstadoLicencia.ACTIVA);
        when(licenciaRepository.findAllByOrderByFechaRegistroDesc()).thenReturn(List.of(licencia2, licencia1));

        List<LicenciaResponse> historico = licenciaService.listarHistorico();

        assertThat(historico).hasSize(2);
        assertThat(historico.get(0).estado()).isEqualTo("ACTIVA");
        assertThat(historico.get(1).estado()).isEqualTo("VENCIDA");
    }

    @Test
    void obtenerVigenteConActivadaPorNuloNoRompe() {
        Licencia licencia = new Licencia();
        licencia.setEstado(EstadoLicencia.ACTIVA);
        when(licenciaRepository.findByEstadoAndFechaInicioLessThanEqualAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoDesc(
                eq(EstadoLicencia.ACTIVA), any(), any())).thenReturn(List.of(licencia));

        LicenciaVigenteResponse respuesta = licenciaService.obtenerVigente();

        assertThat(respuesta.licencia().activadaPorNombre()).isNull();
    }

    @Test
    void observacionOpcionalPuedeQuedarNula() {
        RegistrarLicenciaRequest request = new RegistrarLicenciaRequest(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null);
        when(entityManager.getReference(eq(Usuario.class), any())).thenReturn(mock(Usuario.class));
        when(licenciaRepository.save(any(Licencia.class))).thenAnswer(inv -> inv.getArgument(0));

        LicenciaResponse respuesta = licenciaService.registrar(request, 1L);

        assertThat(respuesta.observacion()).isNull();
    }
}
