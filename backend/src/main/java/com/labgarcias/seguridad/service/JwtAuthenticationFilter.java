package com.labgarcias.seguridad.service;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * CU-01/RN-14: si el header Authorization trae un JWT válido, autentica la
 * request con el id de usuario (sub) y el rol (claim) como authority Spring.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.startsWith(PREFIJO_BEARER)) {
            String token = cabecera.substring(PREFIJO_BEARER.length());
            jwtService.validar(token).ifPresent(this::autenticar);
        }
        filterChain.doFilter(request, response);
    }

    private void autenticar(Claims claims) {
        Long usuarioId = Long.valueOf(claims.getSubject());
        String rol = claims.get("rol", String.class);
        var autoridad = new SimpleGrantedAuthority("ROLE_" + rol);
        var autenticacion = new UsernamePasswordAuthenticationToken(usuarioId, null, List.of(autoridad));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
