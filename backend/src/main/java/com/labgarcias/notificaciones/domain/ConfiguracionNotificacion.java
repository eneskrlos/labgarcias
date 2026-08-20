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
 * usa el conjunto por defecto de §6.3.
 */
@Entity
@Table(name = "configuracion_notificacion")
public class ConfiguracionNotificacion {

    /**
     * §6.3/D-20/D-21: "Sin configuración: app + correo + Telegram para todos".
     *
     * Vive acá y no en el selector porque hay dos lugares que necesitan la misma respuesta: elegir
     * canales al notificar, y contestar el GET de §6.4 de un usuario que nunca configuró nada.
     * Duplicarla sería garantizar que algún día digan cosas distintas.
     *
     * WhatsApp queda afuera a propósito (P-18: es solo estructura).
     */
    public static final Set<Canal> CANALES_POR_DEFECTO = Set.of(Canal.APP, Canal.CORREO, Canal.TELEGRAM);

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

    /**
     * §6.4: qué contestarle a quien todavía no guardó ninguna configuración. No se persiste —no
     * tiene usuario ni id— y su `fechaActualizacion` queda nula, que es justamente lo que dice
     * "esto es el valor por defecto, no algo que hayas elegido".
     */
    public static ConfiguracionNotificacion porDefecto() {
        ConfiguracionNotificacion configuracion = new ConfiguracionNotificacion();
        configuracion.canalAppActivo = CANALES_POR_DEFECTO.contains(Canal.APP);
        configuracion.canalCorreoActivo = CANALES_POR_DEFECTO.contains(Canal.CORREO);
        configuracion.canalTelegramActivo = CANALES_POR_DEFECTO.contains(Canal.TELEGRAM);
        configuracion.canalWhatsappActivo = CANALES_POR_DEFECTO.contains(Canal.WHATSAPP);
        return configuracion;
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
