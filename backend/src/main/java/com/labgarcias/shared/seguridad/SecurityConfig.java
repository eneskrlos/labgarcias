package com.labgarcias.shared.seguridad;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labgarcias.shared.excepcion.EscritorErrorHttp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * spec.md §1.1/§3.1/§3.3: permite sin autenticación Swagger y los endpoints
 * públicos de auth. API stateless (JWT, sin cookies de sesión): CSRF no
 * aplica. Los filtros concretos (JWT en `seguridad`, bloqueo por licencia en
 * `licencia`) se inyectan acá por su supertipo `OncePerRequestFilter` para no
 * incumplir la regla de que `shared` no depende de un módulo de negocio
 * (Agente.md §5.4.3); Spring resuelve cada parámetro por nombre de bean.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * CR-01: sin auto-registro (D-17), sin verificación por correo (D-18) ni Google (D-17).
     * La única alta pública es la solicitud de acceso de §3.1, que no crea ninguna cuenta.
     */
    private static final String[] RUTAS_PUBLICAS = {
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
            "/api/v1/auth/login", "/api/v1/auth/solicitud-acceso"
    };

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** RN-15: las contraseñas se almacenan únicamente como hash BCrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            CorsConfigurationSource corsConfigurationSource,
                                            OncePerRequestFilter jwtAuthenticationFilter,
                                            OncePerRequestFilter licenciaBloqueoFilter,
                                            OncePerRequestFilter cambioPasswordObligatorioFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(licenciaBloqueoFilter, UsernamePasswordAuthenticationFilter.class)
                // §3.1.b: con la contraseña temporal sin cambiar, el token no habilita nada más.
                .addFilterBefore(cambioPasswordObligatorioFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::noAutenticado)
                        .accessDeniedHandler(this::sinPermiso));
        return http.build();
    }

    private void noAutenticado(HttpServletRequest request, HttpServletResponse response, AuthenticationException excepcion)
            throws IOException {
        escribirError(response, HttpServletResponse.SC_UNAUTHORIZED, "NO_AUTENTICADO", "No autenticado.");
    }

    private void sinPermiso(HttpServletRequest request, HttpServletResponse response, AccessDeniedException excepcion)
            throws IOException {
        escribirError(response, HttpServletResponse.SC_FORBIDDEN, "SIN_PERMISO", "No tenés permiso para esta operación.");
    }

    private void escribirError(HttpServletResponse response, int status, String codigo, String mensaje) throws IOException {
        EscritorErrorHttp.escribir(response, objectMapper, status, codigo, mensaje);
    }
}
