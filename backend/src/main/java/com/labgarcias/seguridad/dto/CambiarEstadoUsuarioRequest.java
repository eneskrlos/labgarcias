package com.labgarcias.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * CU-17: alta o baja de una cuenta por el SUPERADMIN.
 *
 * El estado llega como texto y lo convierte el service, no Spring: un valor cualquiera en el enum
 * reventaría el binding y saldría como 500 en vez del 400 con su código. Mismo criterio que el
 * filtro de estado de las solicitudes (§3.1.b) y el `tipoOrden` de §5.7.
 */
public record CambiarEstadoUsuarioRequest(

        @Schema(description = "ACTIVA o INACTIVA. PENDIENTE_VERIFICACION no se acepta: D-18 eliminó "
                + "la verificación por correo y una cuenta ahí quedaría sin forma de destrabarse.",
                example = "INACTIVA")
        @NotBlank(message = "El estado de la cuenta es obligatorio.")
        String estadoCuenta

) {
}
