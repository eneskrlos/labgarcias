package com.labgarcias.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.labgarcias.seguridad.domain.Rol;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;

import io.jsonwebtoken.Claims;

class JwtServiceTest {

    private static final String SECRETO = "clave-de-prueba-con-al-menos-32-caracteres-para-hs256";

    private final JwtService jwtService = new JwtService(SECRETO, 480L);

    private Usuario usuarioDePrueba() {
        Usuario usuario = mock(Usuario.class);
        Rol rol = mock(Rol.class);
        when(rol.getCodigo()).thenReturn(RolCodigo.ODONTOLOGO);
        when(usuario.getId()).thenReturn(7L);
        when(usuario.getRol()).thenReturn(rol);
        return usuario;
    }

    @Test
    void generarProduceUnTokenValidableConElSubYElRolCorrectos() {
        String token = jwtService.generar(usuarioDePrueba());

        Optional<Claims> claims = jwtService.validar(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("7");
        assertThat(claims.get().get("rol", String.class)).isEqualTo("ODONTOLOGO");
    }

    @Test
    void generarFijaLaExpiracionSegunLosMinutosConfigurados() {
        Instant antes = Instant.now();
        String token = jwtService.generar(usuarioDePrueba());
        Instant expiracion = jwtService.validar(token).orElseThrow().getExpiration().toInstant();

        assertThat(expiracion).isAfter(antes.plusSeconds(480 * 60 - 5));
        assertThat(expiracion).isBefore(antes.plusSeconds(480 * 60 + 5));
    }

    @Test
    void validarConTokenMalformadoDevuelveVacio() {
        assertThat(jwtService.validar("esto-no-es-un-jwt")).isEmpty();
    }

    @Test
    void validarConFirmaDeOtraClaveDevuelveVacio() {
        JwtService otroServicio = new JwtService("otra-clave-distinta-tambien-de-32-caracteres!!", 480L);
        String token = otroServicio.generar(usuarioDePrueba());

        assertThat(jwtService.validar(token)).isEmpty();
    }

    @Test
    void validarConTokenVencidoDevuelveVacio() {
        JwtService servicioConExpiracionYaPasada = new JwtService(SECRETO, -1L);
        String tokenVencido = servicioConExpiracionYaPasada.generar(usuarioDePrueba());

        assertThat(jwtService.validar(tokenVencido)).isEmpty();
    }
}
