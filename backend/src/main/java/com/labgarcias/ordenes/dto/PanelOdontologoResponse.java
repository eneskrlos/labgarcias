package com.labgarcias.ordenes.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CU-02: el panel de inicio del odontólogo, entero, en una sola respuesta.
 *
 * RN-01: todo lo que trae es del odontólogo autenticado; el id sale del token y el endpoint no
 * acepta ninguno por parámetro. RN-22: las órdenes recientes identifican al paciente por iniciales
 * y código.
 *
 * **No trae el saludo**: el nombre ya está en la sesión del cliente, y mandarlo de vuelta sería
 * un dato duplicado. **Tampoco trae "mensajes nuevos"** (D-11).
 */
public record PanelOdontologoResponse(

        ContadoresOdontologoResponse contadores,

        @Schema(description = "Últimas órdenes propias por fecha de ingreso, las más nuevas primero")
        List<OrdenListadoResponse> ordenesRecientes

) {
}
