package com.labgarcias.licencia.service;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labgarcias.licencia.repository.LicenciaRepository;
import com.labgarcias.shared.excepcion.ErrorRespuesta;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * PATRÓN: Intercepting Filter
 * PROBLEMA: bloquear toda operación de negocio cuando no hay licencia vigente,
 *           sin repetir la verificación en cada controller.
 * MOTIVADO POR: RN-20 (bloqueo del sistema sin licencia vigente), CU-23,
 *               D-16 (una instalación, sin multi-tenant).
 */
@Component
public class LicenciaBloqueoFilter extends OncePerRequestFilter {

    /** spec.md §3.6: excepciones al bloqueo. */
    private static final List<String> RUTAS_EXENTAS = List.of(
            "/api/v1/auth/login",
            "/api/v1/licencias",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator/health"
    );

    private final LicenciaRepository licenciaRepository;
    private final ObjectMapper objectMapper;

    public LicenciaBloqueoFilter(LicenciaRepository licenciaRepository, ObjectMapper objectMapper) {
        this.licenciaRepository = licenciaRepository;
        this.objectMapper = objectMapper;
    }

    // @NonNull repite el contrato que OncePerRequestFilter ya declara: el contenedor nunca
    // pasa null acá, y dejarlo explícito evita que el análisis lo marque como sin verificar.
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (esRutaExenta(request.getRequestURI()) || licenciaRepository.existeLicenciaVigente()) {
            filterChain.doFilter(request, response);
            return;
        }
        responderLicenciaVencida(response);
    }

    private boolean esRutaExenta(String uri) {
        return RUTAS_EXENTAS.stream().anyMatch(uri::startsWith);
    }

    private void responderLicenciaVencida(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.LOCKED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorRespuesta(
                "LICENCIA_VENCIDA", "La licencia del sistema está vencida. Contactá al SuperAdmin.", null)));
    }
}
