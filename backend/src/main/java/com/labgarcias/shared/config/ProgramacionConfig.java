package com.labgarcias.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas. Hoy la única es el despachador del outbox de notificaciones
 * (§6.1 paso 4), pero @EnableScheduling es una decisión de infraestructura del contenedor y no
 * de un módulo de negocio, así que vive en shared junto al resto de la configuración.
 */
@Configuration
@EnableScheduling
public class ProgramacionConfig {
}
