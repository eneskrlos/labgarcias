package com.labgarcias.notificaciones.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.notificaciones.domain.ConfiguracionNotificacion;
import com.labgarcias.notificaciones.dto.ConfiguracionNotificacionRequest;
import com.labgarcias.notificaciones.dto.ConfiguracionNotificacionResponse;
import com.labgarcias.notificaciones.repository.ConfiguracionNotificacionRepository;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.shared.excepcion.ReglaNegocioException;

import jakarta.persistence.EntityManager;

/**
 * RN-19/CU-21: por qué canales quiere recibir sus notificaciones el usuario autenticado.
 *
 * La configuración es opcional y se crea la primera vez que se guarda: hasta entonces rigen los
 * canales por defecto de §6.3. Por eso el PUT hace alta o edición según corresponda, y no existe
 * un POST aparte —no es un recurso que se cree y se borre, es un ajuste que se tiene o no.
 */
@Service
public class ConfiguracionNotificacionService {

    /** CU-21: un canal activo sin destino sería un envío condenado a fallar en cada notificación. */
    private static final String CODIGO_TELEGRAM_SIN_DESTINO = "TELEGRAM_SIN_DESTINO";

    private final ConfiguracionNotificacionRepository configuracionRepository;
    private final EntityManager entityManager;

    public ConfiguracionNotificacionService(ConfiguracionNotificacionRepository configuracionRepository,
                                            EntityManager entityManager) {
        this.configuracionRepository = configuracionRepository;
        this.entityManager = entityManager;
    }

    /**
     * §6.4: quien nunca configuró nada recibe los valores de §6.3 con `fechaActualizacion` nula,
     * no un 404. Son los canales por los que realmente está recibiendo: negarlos sería mentir.
     */
    @Transactional(readOnly = true)
    public ConfiguracionNotificacionResponse obtenerMia(Long usuarioId) {
        return ConfiguracionNotificacionResponse.de(configuracionRepository.findByUsuarioId(usuarioId)
                .orElseGet(ConfiguracionNotificacion::porDefecto));
    }

    @Transactional
    public ConfiguracionNotificacionResponse guardarMia(Long usuarioId, ConfiguracionNotificacionRequest request) {
        validarDestinoDeTelegram(request);

        ConfiguracionNotificacion configuracion = configuracionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> nuevaDe(usuarioId));
        configuracion.setCanalAppActivo(request.canalAppActivo());
        configuracion.setCanalCorreoActivo(request.canalCorreoActivo());
        configuracion.setCanalTelegramActivo(request.canalTelegramActivo());
        // PUT reemplaza la configuración entera: si el chat no vino, no queda. P-18: whatsapp no se toca.
        configuracion.setTelegramChatId(request.telegramChatId());

        ConfiguracionNotificacion guardada = configuracionRepository.saveAndFlush(configuracion);
        // fecha_actualizacion la pone la base (default al insertar, trigger al actualizar) y la
        // entidad la tiene como no escribible, así que hay que releerla para devolverla al día.
        entityManager.refresh(guardada);
        return ConfiguracionNotificacionResponse.de(guardada);
    }

    /** CU-21 → 422 TELEGRAM_SIN_DESTINO. Un chat en blanco es lo mismo que no tenerlo. */
    private void validarDestinoDeTelegram(ConfiguracionNotificacionRequest request) {
        boolean sinDestino = request.telegramChatId() == null || request.telegramChatId().isBlank();
        if (Boolean.TRUE.equals(request.canalTelegramActivo()) && sinDestino) {
            throw new ReglaNegocioException(CODIGO_TELEGRAM_SIN_DESTINO,
                    "Para recibir por Telegram hace falta indicar el chat de destino.", "telegramChatId");
        }
    }

    private ConfiguracionNotificacion nuevaDe(Long usuarioId) {
        ConfiguracionNotificacion configuracion = new ConfiguracionNotificacion();
        configuracion.setUsuario(entityManager.getReference(Usuario.class, usuarioId));
        return configuracion;
    }
}
