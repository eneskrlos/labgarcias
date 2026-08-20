package com.labgarcias.notificaciones.service;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.ConfiguracionNotificacion;
import com.labgarcias.notificaciones.domain.TipoEvento;
import com.labgarcias.notificaciones.repository.ConfiguracionNotificacionRepository;

/**
 * RN-19: por qué canales sale cada notificación.
 *
 * Son dos preguntas distintas y las dos tienen que dar que sí:
 * - qué canales corresponden al evento → lo declara TipoEvento, copiado de la tabla de §6.2;
 * - qué canales quiere el destinatario → configuracion_notificacion, o el conjunto por
 *   defecto de §6.3 si nunca configuró nada.
 *
 * La intersección es lo que se convierte en filas de notificacion_envio.
 */
@Service
public class SelectorCanales {

    private final ConfiguracionNotificacionRepository configuracionRepository;

    public SelectorCanales(ConfiguracionNotificacionRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    @Transactional(readOnly = true)
    public Set<Canal> canalesDe(TipoEvento tipoEvento, Long destinatarioId) {
        Set<Canal> seleccionados = EnumSet.noneOf(Canal.class);
        seleccionados.addAll(tipoEvento.getCanales());
        seleccionados.retainAll(activosDe(destinatarioId));
        return seleccionados;
    }

    private Set<Canal> activosDe(Long destinatarioId) {
        return configuracionRepository.findByUsuarioId(destinatarioId)
                .map(ConfiguracionNotificacion::canalesActivos)
                .orElse(ConfiguracionNotificacion.CANALES_POR_DEFECTO);
    }
}
