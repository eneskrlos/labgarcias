package com.labgarcias.notificaciones.domain;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.labgarcias.seguridad.domain.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * §6.5 paso 2: el token de un solo uso que viaja en el enlace profundo y que el bot devuelve
 * dentro de `/start {token}`. Es lo único que ata a la persona que le escribe al bot con la
 * cuenta que pidió vincularse.
 *
 * La tabla la creó `V2` (T-29) con `fecha_emision` y `fecha_uso`, y sin columna de vencimiento:
 * la vigencia se calcula sobre la emisión, sin migración.
 */
@Entity
@Table(name = "telegram_token_vinculacion")
public class TelegramTokenVinculacion {

    /** D-21 (§6.5): vigencia del token de vinculación de Telegram. */
    public static final int MINUTOS_VIGENCIA_TOKEN_TELEGRAM = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_emision", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaEmision;

    /** §6.5: se sella al vincular. Un token con fecha de uso ya no vincula a nadie más. */
    @Column(name = "fecha_uso")
    private OffsetDateTime fechaUso;

    public TelegramTokenVinculacion() {
    }

    public TelegramTokenVinculacion(Usuario usuario, String token) {
        this.usuario = usuario;
        this.token = token;
    }

    /**
     * §6.5 criterio 2: un token usado o vencido no vincula. Los dos casos se tratan igual y el
     * bot responde el mismo error, como el token inexistente.
     */
    public boolean estaVigente(OffsetDateTime momento) {
        return fechaUso == null
                && fechaEmision.plusMinutes(MINUTOS_VIGENCIA_TOKEN_TELEGRAM).isAfter(momento);
    }

    public void marcarUsado(OffsetDateTime momento) {
        this.fechaUso = momento;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getToken() {
        return token;
    }

    public OffsetDateTime getFechaEmision() {
        return fechaEmision;
    }

    public OffsetDateTime getFechaUso() {
        return fechaUso;
    }
}
