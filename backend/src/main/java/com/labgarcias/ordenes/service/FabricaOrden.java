package com.labgarcias.ordenes.service;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.labgarcias.catalogos.domain.TipoOrden;
import com.labgarcias.catalogos.domain.TipoTrabajo;
import com.labgarcias.ordenes.domain.Orden;
import com.labgarcias.ordenes.domain.OrdenHistorialEstado;
import com.labgarcias.ordenes.dto.CrearOrdenRequest;
import com.labgarcias.seguridad.domain.Usuario;

/**
 * PATRÓN: Factory Method
 * PROBLEMA: una orden no se arma con los datos que llegan del cliente: nace con estado
 *           inicial, recargo, precios y fechas derivados, y esa variabilidad entre
 *           NORMAL y URGENTE es configuración de la tabla tipo_orden, no código.
 * MOTIVADO POR: RN-11 (estado inicial y recargo por tipo), RN-18 (fecha estimada de
 *               entrega), RN-21 y CU-16 A1 (fotos de precio y días), RN-22 (identificación
 *               del paciente sin nombre completo).
 */
@Component
public class FabricaOrden {

    /** VARCHAR(10) admite hasta 5 iniciales con su punto. */
    private static final int MAXIMO_INICIALES = 5;

    public Orden crear(CrearOrdenRequest request, Usuario odontologo, TipoTrabajo tipoTrabajo, TipoOrden tipoOrden) {
        Orden orden = new Orden();
        orden.setOdontologo(odontologo);
        orden.setPacienteNombre(request.pacienteNombre());
        orden.setPacienteIniciales(derivarIniciales(request.pacienteNombre()));
        orden.setTipoTrabajo(tipoTrabajo);
        orden.setTipoOrden(tipoOrden);
        orden.setDescripcion(request.descripcion());
        orden.setFechaIngreso(request.fechaIngreso());

        // RN-11: estado inicial y recargo se leen de tipo_orden, nunca se deducen del código.
        orden.setEstado(tipoOrden.getEstadoInicial());
        orden.setRecargoUrgencia(tipoOrden.getRecargoMonto());

        // D-14/CU-16 A1: fotos del catálogo vigente al crear la orden.
        orden.setDiasEstimadosAplicados(tipoTrabajo.getDiasEstimados());
        orden.setPrecioBase(tipoTrabajo.getPrecio());

        orden.setFechaEstimadaEntrega(CalculadoraFechaEntrega.sumarDiasHabiles(
                request.fechaIngreso(), tipoTrabajo.getDiasEstimados().intValue()));
        return orden;
    }

    /** §5.1 paso 9: el estado inicial lo asigna el sistema, sin usuario autor. */
    public OrdenHistorialEstado registroInicialDe(Orden orden) {
        OrdenHistorialEstado registro = new OrdenHistorialEstado();
        registro.setOrden(orden);
        registro.setEstado(orden.getEstado());
        registro.setUsuario(null);
        return registro;
    }

    /** RN-22: primera letra de cada palabra, en mayúscula, separadas por punto (Martín Pérez → M.P.). */
    private String derivarIniciales(String pacienteNombre) {
        return Arrays.stream(pacienteNombre.trim().split("\\s+"))
                .filter(palabra -> !palabra.isBlank())
                .limit(MAXIMO_INICIALES)
                .map(palabra -> palabra.substring(0, 1).toUpperCase() + ".")
                .collect(Collectors.joining());
    }
}
