package com.labgarcias.notificaciones.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.notificaciones.domain.ConfiguracionNotificacion;

public interface ConfiguracionNotificacionRepository extends JpaRepository<ConfiguracionNotificacion, Short> {

    /** RN-19: la configuración es opcional; sin fila rige el conjunto por defecto de §6.3. */
    Optional<ConfiguracionNotificacion> findByUsuarioId(Long usuarioId);
}
