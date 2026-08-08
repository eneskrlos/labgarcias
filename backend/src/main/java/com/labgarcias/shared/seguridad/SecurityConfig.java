package com.labgarcias.shared.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * spec.md §1.1: permite sin autenticación las rutas de Swagger.
 * La autenticación real (JWT, @PreAuthorize por rol) se agrega en T-08; hasta
 * entonces el resto de las rutas usa el mecanismo por defecto de Spring Security.
 */
@Configuration
public class SecurityConfig {

    private static final String[] RUTAS_SWAGGER = {
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_SWAGGER).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
