package com.labgarcias.seguridad.service;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labgarcias.shared.excepcion.EscritorErrorHttp;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * PATRÓN: Intercepting Filter
 * PROBLEMA: mientras el odontólogo no cambie su contraseña temporal, su token no puede servir
 *           para nada más. Comprobarlo endpoint por endpoint sería repetir la misma verificación
 *           en cada controller y olvidarla en el próximo que se agregue.
 * MOTIVADO POR: §3.1.b ("el token solo habilita POST /api/v1/auth/cambiar-password") y su
 *               criterio 2 ("hasta cambiar la contraseña, ningún otro endpoint es accesible").
 *
 * Es el mismo mecanismo que el bloqueo por licencia (RN-20), aplicado a una cuenta en vez de a
 * toda la instalación. La restricción se lee del **token**, no del cuerpo ni de un header: el
 * cliente no puede sacársela de encima.
 */
@Component
public class CambioPasswordObligatorioFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";
    private static final String RUTA_CAMBIO_PASSWORD = "/api/v1/auth/cambiar-password";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public CambioPasswordObligatorioFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    // @NonNull repite el contrato que OncePerRequestFilter ya declara: el contenedor nunca
    // pasa null acá, y dejarlo explícito evita que el análisis lo marque como sin verificar.
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (RUTA_CAMBIO_PASSWORD.equals(request.getRequestURI()) || !tokenRestringido(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        responderCambioRequerido(response);
    }

    private boolean tokenRestringido(HttpServletRequest request) {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith(PREFIJO_BEARER)) {
            return false;
        }
        return jwtService.validar(cabecera.substring(PREFIJO_BEARER.length()))
                .map(this::exigeCambio)
                .orElse(false);
    }

    private boolean exigeCambio(Claims claims) {
        return Boolean.TRUE.equals(claims.get(JwtService.CLAVE_DEBE_CAMBIAR_PASSWORD, Boolean.class));
    }

    private void responderCambioRequerido(HttpServletResponse response) throws IOException {
        EscritorErrorHttp.escribir(response, objectMapper, HttpStatus.FORBIDDEN.value(),
                "CAMBIO_PASSWORD_REQUERIDO", "Tenés que cambiar tu contraseña antes de usar el sistema.");
    }
}
