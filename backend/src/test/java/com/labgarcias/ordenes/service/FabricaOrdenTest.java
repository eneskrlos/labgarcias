package com.labgarcias.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.labgarcias.catalogos.domain.Estado;
import com.labgarcias.catalogos.domain.TipoOrden;
import com.labgarcias.catalogos.domain.TipoTrabajo;
import com.labgarcias.ordenes.domain.Orden;
import com.labgarcias.ordenes.domain.OrdenHistorialEstado;
import com.labgarcias.ordenes.dto.CrearOrdenRequest;
import com.labgarcias.seguridad.domain.Usuario;

class FabricaOrdenTest {

    private final FabricaOrden fabricaOrden = new FabricaOrden();

    private TipoTrabajo tipoTrabajo;
    private Usuario odontologo;

    @BeforeEach
    void prepararCatalogo() {
        tipoTrabajo = new TipoTrabajo();
        tipoTrabajo.setNombre("DISYUNTOR CON TORNILLO ESTANDAR");
        tipoTrabajo.setDiasEstimados((short) 7);
        tipoTrabajo.setPrecio(new BigDecimal("250.00"));
        tipoTrabajo.setActivo(true);
        odontologo = new Usuario();
    }

    /** El seed real: NORMAL → estado_inicial RECIBIDO, recargo 0. */
    private TipoOrden tipoOrdenNormal() {
        Estado recibido = new Estado();
        recibido.setCodigo("RECIBIDO");
        recibido.setNombre("Recibido");
        TipoOrden normal = new TipoOrden();
        normal.setNombre("Normal");
        normal.setEstadoInicial(recibido);
        normal.setRecargoMonto(new BigDecimal("0.00"));
        normal.setNotificaAdmin(false);
        return normal;
    }

    /** El seed real: URGENTE → estado_inicial EN_EVALUACION, recargo 200. */
    private TipoOrden tipoOrdenUrgente() {
        Estado enEvaluacion = new Estado();
        enEvaluacion.setCodigo("EN_EVALUACION");
        enEvaluacion.setNombre("En evaluacion");
        TipoOrden urgente = new TipoOrden();
        urgente.setNombre("Urgente");
        urgente.setEstadoInicial(enEvaluacion);
        urgente.setRecargoMonto(new BigDecimal("200.00"));
        urgente.setNotificaAdmin(true);
        return urgente;
    }

    private CrearOrdenRequest request(String pacienteNombre, String tipoOrdenCodigo) {
        return new CrearOrdenRequest(pacienteNombre, LocalDate.of(2026, 8, 6), 16, tipoOrdenCodigo, "Descripción");
    }

    @Test
    void criterio1UnaOrdenNormalNaceEnRecibidoConRecargoCero() {
        Orden orden = fabricaOrden.crear(request("Martín Pérez", "NORMAL"), odontologo, tipoTrabajo, tipoOrdenNormal());

        assertThat(orden.getEstado().getCodigo()).isEqualTo("RECIBIDO");
        assertThat(orden.getRecargoUrgencia()).isEqualByComparingTo("0.00");
    }

    @Test
    void criterio1UnaOrdenUrgenteNaceEnEnEvaluacionConRecargo200() {
        Orden orden = fabricaOrden.crear(request("Martín Pérez", "URGENTE"), odontologo, tipoTrabajo, tipoOrdenUrgente());

        assertThat(orden.getEstado().getCodigo()).isEqualTo("EN_EVALUACION");
        assertThat(orden.getRecargoUrgencia()).isEqualByComparingTo("200.00");
    }

    @Test
    void elEstadoInicialYElRecargoSalenDeLaTablaNoDelCodigoDelTipo() {
        // Un tipo_orden con configuración distinta a la del seed debe respetarse tal cual:
        // si estuviera codificado con if (tipo == URGENTE), este test fallaría.
        Estado listo = new Estado();
        listo.setCodigo("LISTO");
        TipoOrden urgenteReconfigurado = tipoOrdenUrgente();
        urgenteReconfigurado.setEstadoInicial(listo);
        urgenteReconfigurado.setRecargoMonto(new BigDecimal("35.00"));

        Orden orden = fabricaOrden.crear(request("Ana Gómez", "URGENTE"), odontologo, tipoTrabajo, urgenteReconfigurado);

        assertThat(orden.getEstado().getCodigo()).isEqualTo("LISTO");
        assertThat(orden.getRecargoUrgencia()).isEqualByComparingTo("35.00");
    }

    @Test
    void rn22DerivaLasInicialesDelNombreDelPaciente() {
        Orden orden = fabricaOrden.crear(request("Martín Pérez", "NORMAL"), odontologo, tipoTrabajo, tipoOrdenNormal());

        assertThat(orden.getPacienteIniciales()).isEqualTo("M.P.");
    }

    @Test
    void rn22LasInicialesSoportanNombresDeUnaOMasPalabrasYEspaciosDeMas() {
        assertThat(inicialesDe("juan")).isEqualTo("J.");
        assertThat(inicialesDe("  josé  maría   del  valle  ")).isEqualTo("J.M.D.V.");
    }

    @Test
    void rn22LasInicialesNuncaExcedenElLargoDeLaColumna() {
        // paciente_iniciales es VARCHAR(10): un nombre muy largo no puede romper el insert.
        String iniciales = inicialesDe("Ana Beatriz Carla Delia Elena Fabiana Gabriela");

        assertThat(iniciales).hasSizeLessThanOrEqualTo(10);
        assertThat(iniciales).isEqualTo("A.B.C.D.E.");
    }

    private String inicialesDe(String nombre) {
        return fabricaOrden.crear(request(nombre, "NORMAL"), odontologo, tipoTrabajo, tipoOrdenNormal())
                .getPacienteIniciales();
    }

    @Test
    void tomaLaFotoDePrecioYDiasDelCatalogo() {
        Orden orden = fabricaOrden.crear(request("Martín Pérez", "NORMAL"), odontologo, tipoTrabajo, tipoOrdenNormal());

        assertThat(orden.getPrecioBase()).isEqualByComparingTo("250.00");
        assertThat(orden.getDiasEstimadosAplicados()).isEqualTo((short) 7);
    }

    @Test
    void rn18CalculaLaFechaEstimadaEnDiasHabiles() {
        Orden orden = fabricaOrden.crear(request("Martín Pérez", "NORMAL"), odontologo, tipoTrabajo, tipoOrdenNormal());

        assertThat(orden.getFechaIngreso()).isEqualTo(LocalDate.of(2026, 8, 6));
        assertThat(orden.getFechaEstimadaEntrega()).isEqualTo(LocalDate.of(2026, 8, 17));
    }

    @Test
    void guardaElNombreDelPacienteParaUsoInternoYAsociaAlOdontologoAutenticado() {
        Orden orden = fabricaOrden.crear(request("Martín Pérez", "NORMAL"), odontologo, tipoTrabajo, tipoOrdenNormal());

        assertThat(orden.getPacienteNombre()).isEqualTo("Martín Pérez");
        assertThat(orden.getOdontologo()).isSameAs(odontologo);
    }

    @Test
    void p14NoAsignaCargoDeCancelacion() {
        Orden orden = fabricaOrden.crear(request("Martín Pérez", "NORMAL"), odontologo, tipoTrabajo, tipoOrdenNormal());

        assertThat(orden.getCargoCancelacion()).isNull();
        assertThat(orden.getFechaCancelacion()).isNull();
    }

    @Test
    void elRegistroInicialDelHistorialLoAsignaElSistemaSinUsuario() {
        Orden orden = mock(Orden.class);
        Estado recibido = new Estado();
        when(orden.getEstado()).thenReturn(recibido);

        OrdenHistorialEstado registro = fabricaOrden.registroInicialDe(orden);

        assertThat(registro.getOrden()).isSameAs(orden);
        assertThat(registro.getEstado()).isSameAs(recibido);
        assertThat(registro.getUsuario()).isNull();
    }
}
