package com.labgarcias.seguridad.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.labgarcias.seguridad.domain.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** CU-01/RN-14: emisión y validación del JWT de sesión (sub=id de usuario, claim rol). */
@Service
public class JwtService {

    private static final String CLAVE_ROL = "rol";

    /** §3.1.b: token restringido. Va en el token porque es el backend quien tiene que hacerlo cumplir. */
    public static final String CLAVE_DEBE_CAMBIAR_PASSWORD = "debeCambiarPassword";

    private final SecretKey clave;
    private final long minutosExpiracion;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                       @Value("${app.jwt.expiracion-minutos}") long minutosExpiracion) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.minutosExpiracion = minutosExpiracion;
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim(CLAVE_ROL, usuario.getRol().getCodigo().name())
                .claim(CLAVE_DEBE_CAMBIAR_PASSWORD, usuario.isDebeCambiarPassword())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(minutosExpiracion, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    public Optional<Claims> validar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(clave).build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException excepcion) {
            return Optional.empty();
        }
    }
}
