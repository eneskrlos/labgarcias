package com.labgarcias.notificaciones.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.EstadoEnvio;
import com.labgarcias.notificaciones.domain.NotificacionEnvio;
import com.labgarcias.notificaciones.repository.NotificacionEnvioRepository;

/**
 * PATRÓN: Transactional Outbox (lado de entrega) + Strategy
 * PROBLEMA: entregar es lento y puede fallar por causas ajenas al sistema. Hacerlo fuera de la
 *           transacción del negocio, y elegir el canal por configuración y no por código, es lo
 *           que permite que un correo rechazado quede registrado sin afectar nada más.
 * MOTIVADO POR: §6.1 paso 4 (proceso programado que toma los pendientes), RN-19.
 */
@Component
public class DespachadorNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(DespachadorNotificaciones.class);

    private static final String SIN_ADAPTADOR = "Ningún canal atiende los envíos de tipo %s.";
    private static final String SIN_DETALLE = "El canal falló sin informar un motivo.";

    private final NotificacionEnvioRepository envioRepository;
    private final List<CanalNotificacion> canales;
    private final int tamanoLote;

    public DespachadorNotificaciones(NotificacionEnvioRepository envioRepository,
                                     List<CanalNotificacion> canales,
                                     @Value("${app.notificaciones.tamano-lote}") int tamanoLote) {
        this.envioRepository = envioRepository;
        this.canales = canales;
        this.tamanoLote = tamanoLote;
    }

    /**
     * §6.1 paso 4. Detalles que no son obvios:
     *
     * - `fixedDelay` y no `fixedRate`: cuenta desde que termina la corrida anterior, así que dos
     *   despachos no pueden solaparse sobre el mismo lote. Con una única instalación (D-16) eso
     *   alcanza y no hace falta bloquear filas.
     * - Toma **solo PENDIENTE**. §6.1 llama "reintentable" al fallo, pero no hay política de
     *   reintentos documentada —ni tope, ni espera, ni columna de intentos—, y §6.3 dice
     *   explícitamente "sin reintentos automáticos" para el caso de Telegram. Reintentar sin tope
     *   golpearía para siempre a un servidor de correo caído. Los FALLIDO quedan para diagnóstico.
     * - Si el proceso muere a mitad del lote, la transacción se deshace y esos envíos vuelven a
     *   PENDIENTE: se prefiere un aviso repetido a un aviso perdido, que es de lo que trata el outbox.
     */
    @Scheduled(fixedDelayString = "${app.notificaciones.despacho-ms}")
    @Transactional
    public void despachar() {
        envioRepository.findByEstadoEnvioOrderByIdAsc(EstadoEnvio.PENDIENTE, Limit.of(tamanoLote))
                .forEach(this::intentar);
    }

    private void intentar(NotificacionEnvio envio) {
        try {
            canalPara(envio.getCanal()).enviar(envio.getNotificacion());
            envio.marcarEnviado();
        } catch (EnvioNoRealizadoException ex) {
            envio.marcarFallido(detalleDe(ex));
        } catch (RuntimeException ex) {
            // Un fallo no previsto del adaptador no puede tumbar el lote entero ni perder el
            // envío. Se registra con traza para investigarlo, pero sin el texto del mensaje:
            // RN-03/RN-22 valen también para los logs.
            log.error("Fallo inesperado al despachar el envío {} por {}", envio.getId(), envio.getCanal(), ex);
            envio.marcarFallido(detalleDe(ex));
        }
    }

    private CanalNotificacion canalPara(Canal canal) {
        return canales.stream()
                .filter(adaptador -> adaptador.soporta(canal))
                .findFirst()
                .orElseThrow(() -> new EnvioNoRealizadoException(SIN_ADAPTADOR.formatted(canal)));
    }

    private String detalleDe(RuntimeException ex) {
        return ex.getMessage() == null ? SIN_DETALLE : ex.getMessage();
    }
}
