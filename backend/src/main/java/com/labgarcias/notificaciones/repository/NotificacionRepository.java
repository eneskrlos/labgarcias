package com.labgarcias.notificaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.notificaciones.domain.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
}
