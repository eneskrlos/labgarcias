package com.labgarcias.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CambioPasswordObligatorioFilterTest {

    private static final String TOKEN = "un-token";

    @Mock
    private JwtService jwtService;
    @Mock
    private FilterChain filterChain;
    @Mock
    private Claims claims;

    private CambioPasswordObligatorioFilter construirFiltro() {
        return new CambioPasswordObligatorioFilter(jwtService, new ObjectMapper());
    }

    private MockHttpServletRequest requestConToken(String metodo, String ruta) {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }

    private void elTokenExigeCambio(boolean exige) {
        when(claims.get(JwtService.CLAVE_DEBE_CAMBIAR_PASSWORD, Boolean.class)).thenReturn(exige);
        when(jwtService.validar(TOKEN)).thenReturn(Optional.of(claims));
    }

    /** §3.1.b criterio 2: con la contraseña sin cambiar, ningún otro endpoint responde. */
    @ParameterizedTest(name = "bloquea {0}")
    @ValueSource(strings = {
            "/api/v1/ordenes",
            "/api/v1/notificaciones",
            "/api/v1/notificaciones/contador",
            "/api/v1/perfil",
            "/api/v1/auth/logout"
    })
    void criterio2ElTokenRestringidoNoAbreNingunOtroEndpoint(String ruta) throws Exception {
        elTokenExigeCambio(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        construirFiltro().doFilter(requestConToken("GET", ruta), response, filterChain);

        verify(filterChain, never()).doFilter(anyRequest(), anyResponse());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("CAMBIO_PASSWORD_REQUERIDO");
    }

    /** El mensaje se escribe fuera del @RestControllerAdvice: el charset se fija a mano. */
    @Test
    void elMensajeLlegaEnUtf8ConLosAcentosIntactos() throws Exception {
        elTokenExigeCambio(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        construirFiltro().doFilter(requestConToken("GET", "/api/v1/ordenes"), response, filterChain);

        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        assertThat(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .contains("Tenés que cambiar tu contraseña antes de usar el sistema.");
    }

    /** §3.1.b: la única puerta abierta es el propio cambio de contraseña. */
    @Test
    void dejaPasarElCambioDePassword() throws Exception {
        elTokenExigeCambio(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        construirFiltro().doFilter(
                requestConToken("POST", "/api/v1/auth/cambiar-password"), response, filterChain);

        verify(filterChain).doFilter(anyRequest(), anyResponse());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    /** Un token normal no se ve afectado por este filtro. */
    @Test
    void unTokenSinLaBanderaPasaDeLargo() throws Exception {
        elTokenExigeCambio(false);

        construirFiltro().doFilter(
                requestConToken("GET", "/api/v1/ordenes"), new MockHttpServletResponse(), filterChain);

        verify(filterChain).doFilter(anyRequest(), anyResponse());
    }

    /** Sin token, decide la cadena de seguridad; este filtro no se mete. */
    @Test
    void unaRequestSinTokenNoSeBloqueaAca() throws Exception {
        construirFiltro().doFilter(
                new MockHttpServletRequest("POST", "/api/v1/auth/login"), new MockHttpServletResponse(), filterChain);

        verify(filterChain).doFilter(anyRequest(), anyResponse());
        verify(jwtService, never()).validar(anyString());
    }

    /** Un token ilegible no es asunto de este filtro: lo rechaza la autenticación. */
    @Test
    void unTokenInvalidoPasaDeLargo() throws Exception {
        when(jwtService.validar(TOKEN)).thenReturn(Optional.empty());

        construirFiltro().doFilter(
                requestConToken("GET", "/api/v1/ordenes"), new MockHttpServletResponse(), filterChain);

        verify(filterChain).doFilter(anyRequest(), anyResponse());
    }

    private static jakarta.servlet.ServletRequest anyRequest() {
        return org.mockito.ArgumentMatchers.any(jakarta.servlet.ServletRequest.class);
    }

    private static jakarta.servlet.ServletResponse anyResponse() {
        return org.mockito.ArgumentMatchers.any(jakarta.servlet.ServletResponse.class);
    }
}
