package com.labgarcias.seguridad.domain;

/** CU-18/CU-19: ciclo de vida de la cuenta (chk_usuario_estado). */
public enum EstadoCuenta {
    PENDIENTE_VERIFICACION,
    ACTIVA,
    INACTIVA
}
