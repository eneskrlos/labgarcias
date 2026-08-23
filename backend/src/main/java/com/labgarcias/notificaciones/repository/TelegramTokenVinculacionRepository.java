package com.labgarcias.notificaciones.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.notificaciones.domain.TelegramTokenVinculacion;

public interface TelegramTokenVinculacionRepository extends JpaRepository<TelegramTokenVinculacion, Long> {

    /**
     * §6.5 paso 4: lo único que el bot trae es el token. Se carga el usuario en la misma consulta
     * porque el paso siguiente es siempre vincularlo.
     */
    @EntityGraph(attributePaths = "usuario")
    Optional<TelegramTokenVinculacion> findByToken(String token);
}
