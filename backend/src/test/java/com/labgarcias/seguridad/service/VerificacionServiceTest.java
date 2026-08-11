package com.labgarcias.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.TokenVerificacion;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.repository.TokenVerificacionRepository;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.excepcion.ValidacionException;
import com.labgarcias.shared.util.ConstantesDominio;

@ExtendWith(MockitoExtension.class)
class VerificacionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenVerificacionRepository tokenVerificacionRepository;

    @InjectMocks
    private VerificacionService verificacionService;

    @Test
    void generarTokenCreaUnTokenValidoPor24Horas() {
        Usuario usuario = new Usuario();
        when(tokenVerificacionRepository.save(any(TokenVerificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        TokenVerificacion token = verificacionService.generarToken(usuario);

        assertThat(token.getUsuario()).isSameAs(usuario);
        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getFechaUso()).isNull();
        assertThat(token.getFechaExpiracion())
                .isEqualTo(token.getFechaEmision().plusHours(ConstantesDominio.HORAS_VIGENCIA_TOKEN));
    }

    @Test
    void verificarConTokenInexistenteLanzaTokenInvalido() {
        when(tokenVerificacionRepository.findByToken("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificacionService.verificar("no-existe"))
                .isInstanceOf(ValidacionException.class)
                .satisfies(ex -> assertThat(((ValidacionException) ex).getCodigo()).isEqualTo("TOKEN_INVALIDO"));
    }

    @Test
    void verificarConTokenYaUsadoLanzaTokenInvalido() {
        TokenVerificacion token = new TokenVerificacion();
        token.setFechaExpiracion(OffsetDateTime.now().plusHours(1));
        token.setFechaUso(OffsetDateTime.now().minusMinutes(5));
        when(tokenVerificacionRepository.findByToken("usado")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> verificacionService.verificar("usado"))
                .isInstanceOf(ValidacionException.class)
                .satisfies(ex -> assertThat(((ValidacionException) ex).getCodigo()).isEqualTo("TOKEN_INVALIDO"));
    }

    @Test
    void verificarConTokenVencidoLanzaTokenInvalido() {
        TokenVerificacion token = new TokenVerificacion();
        token.setFechaExpiracion(OffsetDateTime.now().minusMinutes(1));
        when(tokenVerificacionRepository.findByToken("vencido")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> verificacionService.verificar("vencido"))
                .isInstanceOf(ValidacionException.class)
                .satisfies(ex -> assertThat(((ValidacionException) ex).getCodigo()).isEqualTo("TOKEN_INVALIDO"));
    }

    @Test
    void verificarConTokenValidoActivaLaCuenta() {
        Usuario usuario = new Usuario();
        usuario.setEstadoCuenta(EstadoCuenta.PENDIENTE_VERIFICACION);
        usuario.setCorreoVerificado(false);

        TokenVerificacion token = new TokenVerificacion();
        token.setUsuario(usuario);
        token.setFechaExpiracion(OffsetDateTime.now().plusHours(1));
        when(tokenVerificacionRepository.findByToken("valido")).thenReturn(Optional.of(token));

        verificacionService.verificar("valido");

        assertThat(token.getFechaUso()).isNotNull();
        assertThat(usuario.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(usuario.isCorreoVerificado()).isTrue();
    }

    @Test
    void reenviarVerificacionParaCorreoInexistenteNoHaceNadaNiFalla() {
        when(usuarioRepository.findByCorreoIgnoreCase("no.existe@mail.com")).thenReturn(Optional.empty());

        verificacionService.reenviarVerificacion("no.existe@mail.com");

        verify(tokenVerificacionRepository, never()).findByUsuarioIdAndFechaUsoIsNull(any());
        verify(tokenVerificacionRepository, never()).save(any());
    }

    @Test
    void reenviarVerificacionInvalidaPendientesYGeneraUnoNuevo() {
        Usuario usuario = new Usuario();
        when(usuarioRepository.findByCorreoIgnoreCase("juan@mail.com")).thenReturn(Optional.of(usuario));

        TokenVerificacion pendiente1 = new TokenVerificacion();
        TokenVerificacion pendiente2 = new TokenVerificacion();
        when(tokenVerificacionRepository.findByUsuarioIdAndFechaUsoIsNull(any()))
                .thenReturn(List.of(pendiente1, pendiente2));
        when(tokenVerificacionRepository.save(any(TokenVerificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        verificacionService.reenviarVerificacion("juan@mail.com");

        assertThat(pendiente1.getFechaUso()).isNotNull();
        assertThat(pendiente2.getFechaUso()).isNotNull();

        ArgumentCaptor<TokenVerificacion> captor = ArgumentCaptor.forClass(TokenVerificacion.class);
        verify(tokenVerificacionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isSameAs(usuario);
    }
}
