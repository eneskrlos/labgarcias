package com.labgarcias.notificaciones.service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.notificaciones.domain.TelegramTokenVinculacion;
import com.labgarcias.notificaciones.dto.VinculacionTelegramResponse;
import com.labgarcias.notificaciones.repository.TelegramTokenVinculacionRepository;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.service.UsuarioService;
import com.labgarcias.shared.excepcion.ReglaNegocioException;

/**
 * §6.5: la vinculación, del lado del sistema. Un bot de Telegram no puede iniciar una
 * conversación, así que el usuario tiene que escribirle primero; el token de un solo uso es lo
 * que permite reconocer, del otro lado, qué cuenta es la que está escribiendo.
 *
 * La escritura de `usuario.telegram_chat_id` se delega a `UsuarioService`: la entidad es del
 * módulo `seguridad` y quien la modifica es su propio servicio (Agente.md 5.4).
 */
@Service
public class VinculacionTelegramService {

    private static final String ENLACE_PROFUNDO = "https://t.me/%s?start=%s";

    /** 24 bytes al azar, base64 URL-safe: 32 caracteres, dentro del VARCHAR(64) de V2. */
    private static final int BYTES_TOKEN = 24;

    private static final String NO_CONFIGURADO =
            "La vinculación con Telegram no está disponible: falta configurar el bot (P-20).";

    private final TelegramTokenVinculacionRepository tokenRepository;
    private final UsuarioService usuarioService;
    private final ClienteTelegram clienteTelegram;
    private final SecureRandom aleatorio = new SecureRandom();
    private final Base64.Encoder codificador = Base64.getUrlEncoder().withoutPadding();

    public VinculacionTelegramService(TelegramTokenVinculacionRepository tokenRepository,
                                      UsuarioService usuarioService,
                                      ClienteTelegram clienteTelegram) {
        this.tokenRepository = tokenRepository;
        this.usuarioService = usuarioService;
        this.clienteTelegram = clienteTelegram;
    }

    /**
     * §6.5 paso 2: emite el token y devuelve el enlace que abre la conversación con el bot ya
     * cargada con `/start {token}`.
     */
    @Transactional
    public VinculacionTelegramResponse generarEnlace(Long usuarioId) {
        if (!clienteTelegram.vinculacionHabilitada()) {
            throw new ReglaNegocioException("TELEGRAM_NO_CONFIGURADO", NO_CONFIGURADO);
        }
        Usuario usuario = usuarioService.obtenerPorId(usuarioId);
        TelegramTokenVinculacion emitido =
                tokenRepository.save(new TelegramTokenVinculacion(usuario, generarToken()));
        return new VinculacionTelegramResponse(
                ENLACE_PROFUNDO.formatted(clienteTelegram.getNombreBot(), emitido.getToken()));
    }

    /**
     * §6.5 paso 4: llega el `/start {token}` desde el bot. Devuelve si la cuenta quedó vinculada;
     * **false** cubre los tres casos del criterio 2 —token inexistente, ya usado o vencido—, que
     * se responden igual: distinguirlos le diría a quien prueba tokens cuáles existieron.
     */
    @Transactional
    public boolean vincular(String token, String chatId) {
        OffsetDateTime ahora = OffsetDateTime.now();
        return tokenRepository.findByToken(token)
                .filter(emitido -> emitido.estaVigente(ahora))
                .map(emitido -> {
                    emitido.marcarUsado(ahora);
                    usuarioService.vincularTelegram(emitido.getUsuario().getId(), chatId);
                    return true;
                })
                .orElse(false);
    }

    /** §6.5 paso 5: desvincular corta los envíos sin tocar el correo ni la campana (criterio 3). */
    @Transactional
    public void desvincular(Long usuarioId) {
        usuarioService.desvincularTelegram(usuarioId);
    }

    private String generarToken() {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        return codificador.encodeToString(bytes);
    }
}
