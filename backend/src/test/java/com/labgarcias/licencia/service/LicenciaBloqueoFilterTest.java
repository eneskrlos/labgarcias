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
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("LICENCIA_VENCIDA");
    }
}
