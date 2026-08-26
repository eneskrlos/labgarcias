package com.labgarcias.ordenes.service;

import org.springframework.stereotype.Component;

import com.labgarcias.ordenes.domain.Orden;
import com.labgarcias.ordenes.dto.OrdenListadoResponse;

/**
 * RN-03/RN-22: el único lugar donde se arma la identificación del paciente y el ítem de un listado.
 *
 * Vive aparte porque lo necesitan dos services del módulo —el de órdenes y el de los paneles— y
 * duplicarlo sería duplicar justamente la regla que impide que el nombre del paciente salga en una
 * lista (`Agente.md` §6.2 y §8.2). No es una capa de mapeo genérica: son estas dos conversiones,
 * con estos dos usos reales.
 */
@Component
public class MapeadorOrden {

    public OrdenListadoResponse aItemDeListado(Orden orden) {
        return new OrdenListadoResponse(
                orden.getId(),
                orden.getCodigo(),
                identificacionPaciente(orden),
                orden.getTipoTrabajo().getNombre(),
                orden.getTipoOrden().getNombre(),
                orden.getEstado().getNombre(),
                orden.getEstado().getCodigo(),
                orden.getFechaIngreso(),
                orden.getFechaEstimadaEntrega(),
                orden.getPrecioTotal());
    }

    /** RN-03/RN-22: al paciente se lo identifica por iniciales y código, nunca por su nombre. */
    public String identificacionPaciente(Orden orden) {
        return orden.getPacienteIniciales() + " - Caso #" + orden.getPacienteCodigo();
    }
}
