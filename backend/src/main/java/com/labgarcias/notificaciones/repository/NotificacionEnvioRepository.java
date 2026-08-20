package com.labgarcias.notificaciones.repository;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.notificaciones.domain.EstadoEnvio;
import com.labgarcias.notificaciones.domain.NotificacionEnvio;

public interface NotificacionEnvioRepository extends JpaRepository<NotificacionEnvio, Long> {

    /**
     * §6.1 paso 4: la cola del despachador. Trae la notificación y su destinatario en la misma
     * consulta porque cada canal los necesita —el correo, para saber a quién escribirle— y sin
     * el grafo cada envío del lote dispararía dos consultas más.
     *
     * En orden de id: el más viejo primero. `Limit` acota el lote sin arrastrar el aparato de
     * paginación, que acá no aporta nada (nadie muestra estos registros).
     */
    @EntityGraph(attributePaths = { "notificacion", "notificacion.destinatario" })
    List<NotificacionEnvio> findByEstadoEnvioOrderByIdAsc(EstadoEnvio estadoEnvio, Limit limite);
}
