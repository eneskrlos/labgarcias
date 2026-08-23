package com.labgarcias.licencia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labgarcias.licencia.repository.LicenciaRepository;

import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
class LicenciaBloqueoFilterTest {

    @Mock
    private LicenciaRepository licenciaRepository;

    @Mock
    private FilterChain filterChain;

    private LicenciaBloqueoFilter construirFiltro() {
        return new LicenciaBloqueoFilter(licenciaRepository, new ObjectMapper());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/login",
            "/api/v1/licencias",
            "/api/v1/licencias/vigente",
            "/swagger-ui/index.html",
            "/v3/api-docs",
            "/actuator/health"
    })
    void rutasExentasPasanSinConsultarLaLicencia(String ruta) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", ruta);
        request.setRequestURI(ruta);
        MockHttpServletResponse response = new MockHttpServletResponse();

        construirFiltro().doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(licenciaRepository, never()).existeLicenciaVigente();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rutaDeNegocioConLicenciaVigentePasaElFiltro() throws Exception {
        when(licenciaRepository.existeLicenciaVigente()).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/registro");
        request.setRequestURI("/api/v1/auth/registro");
        MockHttpServletResponse response = new MockHttpServletResponse();

        construirFiltro().doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rutaDeNegocioSinLicenciaDevuelve423ConCuerpoUniformeYNoContinuaLaCadena() throws Exception {
        when(licenciaRepository.existeLicenciaVigente()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/registro");
        request.setRequestURI("/api/v1/auth/registro");
        MockHttpServletResponse response = new MockHttpServletResponse();

        construirFiltro().doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(423);
        // El Content-Type incluye ahora ";charset=UTF-8": es lo que hace legibles los acentos.
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("LICENCIA_VENCIDA");
    }

    /**
     * El mensaje se escribe fuera del @RestControllerAdvice, así que el charset hay que fijarlo a
     * mano. Sin eso, `getWriter()` usa ISO-8859-1 y "está vencida" llega ilegible al cliente.
     */
    @Test
    void elMensajeLlegaEnUtf8ConLosAcentosIntactos() throws Exception {
        when(licenciaRepository.existeLicenciaVigente()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/ordenes");
        request.setRequestURI("/api/v1/ordenes");
        MockHttpServletResponse response = new MockHttpServletResponse();

        construirFiltro().doFilterInternal(request, response, filterChain);

        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        assertThat(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .contains("La licencia del sistema está vencida. Contactá al SuperAdmin.");
    }
}
