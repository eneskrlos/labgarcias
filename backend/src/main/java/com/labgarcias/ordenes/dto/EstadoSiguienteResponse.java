package com.labgarcias.ordenes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * §5.5/§8: la única transición que la orden admite ahora mismo, tal como la calcula el backend.
 *
 * Va con **código y nombre**: el código es lo que hay que mandar en el `PATCH`, y el nombre es lo
 * que muestra el botón. El nombre viaja en vez de derivarse del código porque `estado.nombre` es
 * editable (CU-22): si el administrador renombra una etapa, la pantalla tiene que seguirlo.
 */
public record EstadoSiguienteResponse(

        @Schema(description = "Código a enviar en PATCH /ordenes/{id}/estado", example = "EN_PRODUCCION")
        String codigo,

        @Schema(description = "Nombre visible de la etapa, editable por CU-22", example = "En produccion")
        String nombre

) {
}
