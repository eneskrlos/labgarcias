package com.labgarcias.shared.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * spec.md §1.1/§3.1: permite sin autenticación Swagger y los endpoints públicos de auth.
 * API stateless (JWT, sin formularios ni cookies de sesión): CSRF no aplica y no
 * se crean sesiones HTTP. La autenticación real (JWT, @PreAuthorize por rol) se
 * agrega en T-08; hasta entonces el resto de las rutas usa el mecanismo por
 * defecto de Spring Security.
 */
@Configuration
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
            "/api/v1/auth/registro", "/api/v1/auth/verificar", "/api/v1/auth/reenviar-verificacion"
    };

    /** RN-15: las contraseñas se almacenan únicamente como hash BCrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
