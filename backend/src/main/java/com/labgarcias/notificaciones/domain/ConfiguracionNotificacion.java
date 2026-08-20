package com.labgarcias.notificaciones.domain;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;

import com.labgarcias.seguridad.domain.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * RN-19/CU-21: por qué canales quiere recibir cada usuario. Es opcional: quien no tiene fila
 * usa el conjunto por defecto de §6.3 (ver SelectorCanales).
 *
 * `telegram_chat_id` y la validación de CU-21 (activar Telegram sin destino) pertenecen a los
 * endpoints de §6.4, que son T-22. Acá solo se lee.
 */
@Entity
@Table(name = "configuracion_notificacion")
public class ConfiguracionNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "canal_app_activo", nullable = false)
    private boolean canalAppActivo;

    @Column(name = "canal_correo_activo", nullable = false)
    private boolean canalCorreoActivo;

    @Column(name = "canal_telegram_activo", nullable = false)
    private boolean canalTelegramActivo;

    /** P-18/D-21: la columna existe desde V2, pero el canal es solo estructura. */
    @Column(name = "canal_whatsapp_activo", nullable = false)
    private boolean canalWhatsappActivo;

    @Column(name = "telegram_chat_id", length = 100)
    private String telegramChatId;

    @Column(name = "fecha_actualizacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;

    public ConfiguracionNotificacion() {
    }

    /** RN-19: traduce las cuatro banderas al conjunto que el selector sabe cruzar. */
    public Set<Canal> canalesActivos() {
        Set<Canal> activos = EnumSet.noneOf(Canal.class);
        if (canalAppActivo) {
            activos.add(Canal.APP);
        }
        if (canalCorreoActivo) {
            activos.add(Canal.CORREO);
        }
        if (canalTelegramActivo) {
            activos.add(Canal.TELEGRAM);
        }
        if (canalWhatsappActivo) {
            activos.add(Canal.WHATSAPP);
        }
        return activos;
    }

    public Short getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public boolean isCanalAppActivo() {
        return canalAppActivo;
    }

    public void setCanalAppActivo(boolean canalAppActivo) {
        this.canalAppActivo = canalAppActivo;
    }

    public boolean isCanalCorreoActivo() {
        return canalCorreoActivo;
    }

    public void setCanalCorreoActivo(boolean canalCorreoActivo) {
        this.canalCorreoActivo = canalCorreoActivo;
    }

    public boolean isCanalTelegramActivo() {
        return canalTelegramActivo;
    }

    public void setCanalTelegramActivo(boolean canalTelegramActivo) {
        this.canalTelegramActivo = canalTelegramActivo;
    }

    public boolean isCanalWhatsappActivo() {
        return canalWhatsappActivo;
    }

    public void setCanalWhatsappActivo(boolean canalWhatsappActivo) {
        this.canalWhatsappActivo = canalWhatsappActivo;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
