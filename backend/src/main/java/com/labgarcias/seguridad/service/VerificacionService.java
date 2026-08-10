package com.labgarcias.seguridad.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.TokenVerificacion;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.repository.TokenVerificacionRepository;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.excepcion.ValidacionException;
import com.labgarcias.shared.util.ConstantesDominio;

/** CU-19/D-02: emisión, envío y consumo del token de verificación de cuenta. */
@Service
public class VerificacionService {

    private static final Logger log = LoggerFactory.getLogger(VerificacionService.class);

    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacionRepository tokenVerificacionRepository;

    public VerificacionService(UsuarioRepository usuarioRepository,
                                TokenVerificacionRepository tokenVerificacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tokenVerificacionRepository = tokenVerificacionRepository;
    }

    /** Usado por el registro (CU-18) y por el reenvío (CU-19 A1). */
    @Transactional
    public TokenVerificacion generarToken(Usuario usuario) {
        OffsetDateTime ahora = OffsetDateTime.now();
        TokenVerificacion token = new TokenVerificacion();
        token.setUsuario(usuario);
        token.setToken(UUID.randomUUID().toString());
        token.setFechaEmision(ahora);
        token.setFechaExpiracion(ahora.plusHours(ConstantesDominio.HORAS_VIGENCIA_TOKEN));
        TokenVerificacion guardado = tokenVerificacionRepository.save(token);
        enviarCorreoVerificacion(usuario, guardado);
        return guardado;
    }

    @Transactional
    public void verificar(String tokenValor) {
        TokenVerificacion token = tokenVerificacionRepository.findByToken(tokenValor)
                .orElseThrow(this::tokenInvalido);

        boolean vencido = token.getFechaExpiracion().isBefore(OffsetDateTime.now());
        boolean usado = token.getFechaUso() != null;
        if (vencido || usado) {
            throw tokenInvalido();
        }

        token.setFechaUso(OffsetDateTime.now());

        Usuario usuario = token.getUsuario();
        usuario.setEstadoCuenta(EstadoCuenta.ACTIVA);
        usuario.setCorreoVerificado(true);
    }

    /** CU-19 A1: responde igual exista o no la cuenta, para no revelarla (RN-22 en espíritu). */
    @Transactional
    public void reenviarVerificacion(String correo) {
        usuarioRepository.findByCorreoIgnoreCase(correo).ifPresent(usuario -> {
            invalidarTokensPendientes(usuario);
            generarToken(usuario);
        });
    }

    private void invalidarTokensPendientes(Usuario usuario) {
        OffsetDateTime ahora = OffsetDateTime.now();
        tokenVerificacionRepository.findByUsuarioIdAndFechaUsoIsNull(usuario.getId())
                .forEach(token -> token.setFechaUso(ahora));
    }

    /**
     * T-07: envío mínimo por log. El adaptador real (CanalCorreo/SMTP) llega en T-21
     * junto con el resto del módulo de notificaciones.
     */
    private void enviarCorreoVerificacion(Usuario usuario, TokenVerificacion token) {
        log.info("Verificación de cuenta para {}: token={} (vence {})",
                usuario.getCorreo(), token.getToken(), token.getFechaExpiracion());
    }

    private ValidacionException tokenInvalido() {
        return new ValidacionException("TOKEN_INVALIDO", "El enlace de verificación no es válido, venció o ya fue usado.", "token");
    }
}
