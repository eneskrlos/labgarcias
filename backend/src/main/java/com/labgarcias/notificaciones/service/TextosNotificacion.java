package com.labgarcias.notificaciones.service;

import com.labgarcias.notificaciones.domain.TipoEvento;

/**
 * Único lugar donde se decide qué dice una notificación.
 *
 * RN-03/RN-22: al paciente se lo nombra por su código, nunca por su nombre. `notificacion.mensaje`
 * se persiste y además viaja por correo y Telegram, así que es exactamente el tipo de texto donde
 * un nombre de paciente no puede aparecer.
 *
 * §3.1.b: por la misma razón, ningún texto de acá puede llevar una contraseña temporal. El correo
 * de credenciales se compone aparte, fuera del outbox.
 */
public final class TextosNotificacion {

    /** CU-07: formato documentado, palabra por palabra. No cambiarlo sin cambiar la spec. */
    private static final String CAMBIO_ESTADO = "El trabajo del paciente Código %d pasó a la etapa de %s.";

    /** §5.1 paso 10. Sin texto documentado: calcado del formato de CU-07. */
    private static final String NUEVA_ORDEN = "Se registró la orden %s del paciente Código %d.";

    /** RN-11. Sin texto documentado: calcado del formato de CU-07. */
    private static final String ORDEN_URGENTE = "Se registró la orden urgente %s del paciente Código %d.";

    private static final String ASUNTO_CAMBIO_ESTADO = "Lab. Garcia's Connect — Tu trabajo avanzó de etapa";
    private static final String ASUNTO_NUEVA_ORDEN = "Lab. Garcia's Connect — Orden registrada";
    private static final String ASUNTO_ORDEN_URGENTE = "Lab. Garcia's Connect — Nueva orden urgente";
    private static final String ASUNTO_GENERICO = "Lab. Garcia's Connect — Aviso";

    private TextosNotificacion() {
    }

    public static String cambioEstado(Integer pacienteCodigo, String estadoNombre) {
        return CAMBIO_ESTADO.formatted(pacienteCodigo, estadoNombre);
    }

    public static String nuevaOrden(String codigoOrden, Integer pacienteCodigo) {
        return NUEVA_ORDEN.formatted(codigoOrden, pacienteCodigo);
    }

    public static String ordenUrgente(String codigoOrden, Integer pacienteCodigo) {
        return ORDEN_URGENTE.formatted(codigoOrden, pacienteCodigo);
    }

    /**
     * §6.3: el asunto del correo no está documentado en ningún caso de uso. Los tres eventos
     * que hoy se emiten tienen el suyo; SOLICITUD_ACCESO (T-30) y CREDENCIALES_CREADAS (T-31)
     * definirán el propio cuando esas tareas los emitan.
     */
    public static String asuntoDe(TipoEvento tipoEvento) {
        return switch (tipoEvento) {
            case CAMBIO_ESTADO -> ASUNTO_CAMBIO_ESTADO;
            case NUEVA_ORDEN -> ASUNTO_NUEVA_ORDEN;
            case ORDEN_URGENTE -> ASUNTO_ORDEN_URGENTE;
            default -> ASUNTO_GENERICO;
        };
    }
}
