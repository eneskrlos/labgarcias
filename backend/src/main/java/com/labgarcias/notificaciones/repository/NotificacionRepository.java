package com.labgarcias.notificaciones.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labgarcias.notificaciones.domain.Notificacion;

/**
 * §6 criterio 3: "un usuario solo ve sus propias notificaciones".
 *
 * Ese criterio está garantizado por la forma de estas consultas, no por una verificación posterior:
 * **todas** filtran por `destinatarioId`, incluida la que busca una notificación puntual. No existe
 * un `findById` suelto que alguien pueda usar por descuido y devolver la notificación de otro.
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByDestinatarioId(Long destinatarioId, Pageable pageable);

    Page<Notificacion> findByDestinatarioIdAndLeida(Long destinatarioId, boolean leida, Pageable pageable);

    /** §6.4: lo que muestra la campana. */
    long countByDestinatarioIdAndLeidaFalse(Long destinatarioId);

    /**
     * La notificación ajena no se distingue de la inexistente: las dos devuelven vacío y terminan
     * en 404. Un 403 delataría que el id existe (mismo criterio que RN-01 para las órdenes).
     */
    Optional<Notificacion> findByIdAndDestinatarioId(Long id, Long destinatarioId);

    /**
     * §6.4 "leer-todas". Va como update masivo y no como un bucle de entidades: quien tiene 200
     * avisos sin leer no puede costar 200 consultas para vaciar la campana de un clic.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLectura = :fechaLectura "
            + "WHERE n.destinatario.id = :destinatarioId AND n.leida = false")
    int marcarTodasLeidas(@Param("destinatarioId") Long destinatarioId,
                          @Param("fechaLectura") OffsetDateTime fechaLectura);
}
