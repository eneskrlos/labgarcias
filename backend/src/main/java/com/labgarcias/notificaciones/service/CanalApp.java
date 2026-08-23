package com.labgarcias.notificaciones.service;

import org.springframework.stereotype.Component;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.Notificacion;

/**
 * PATRÓN: Adapter (implementación de CanalNotificacion)
 * PROBLEMA: la campana de §6.4 no necesita ninguna entrega: lee la tabla `notificacion`, que
 *           ya se escribió al registrar el evento. Sin este adaptador habría que exceptuar a
 *           APP en el despachador con un `if`, y el canal quedaría sin traza de envío.
 * MOTIVADO POR: RN-05 (la notificación en la aplicación), §6.3 (CanalApp marca ENVIADO).
 */
@Component
public class CanalApp implements CanalNotificacion {

    @Override
    public boolean soporta(Canal canal) {
        return canal == Canal.APP;
    }

    /**
     * §6.3: no hace nada, y eso es lo correcto. Que termine sin excepción es lo que deja el
     * envío en ENVIADO, así que el registro por canal de RN-05 queda completo igual que los otros.
     */
    @Override
    public void enviar(Notificacion notificacion) {
        // La notificación ya está persistida: la campana la lee de ahí.
    }
}
