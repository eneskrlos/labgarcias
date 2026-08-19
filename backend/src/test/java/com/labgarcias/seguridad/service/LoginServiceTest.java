package com.labgarcias.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.Rol;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.dto.LoginRequest;
import com.labgarcias.seguridad.dto.LoginResponse;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.excepcion.AccesoDenegadoException;
import com.labgarcias.shared.excepcion.NoAutenticadoException;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final String CORREO = "juan@mail.com";
    private static final String PASSWORD = "*38Op5)l6";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private LoginService loginService;

    @Test
    void correoInexistenteYPasswordIncorrectaDevuelvenElMismoError() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest(CORREO, PASSWORD);

        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(NoAutenticadoException.class)
                .satisfies(ex -> assertThat(((NoAutenticadoException) ex).getCodigo()).isEqualTo("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void passwordIncorrectaLanzaElMismoCodigoQueCorreoInexistente() {
        Usuario usuario = usuarioLocalActivo();
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD, usuario.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(new LoginRequest(CORREO, PASSWORD)))
                .isInstanceOf(NoAutenticadoException.class)
                .satisfies(ex -> assertThat(((NoAutenticadoException) ex).getCodigo()).isEqualTo("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void usuarioDeGoogleSinPasswordHashNoPuedeLoguearseLocalmenteYNoRompe() {
        Usuario usuarioGoogle = new Usuario();
        usuarioGoogle.setPasswordHash(null);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuarioGoogle));

        assertThatThrownBy(() -> loginService.login(new LoginRequest(CORREO, PASSWORD)))
                .isInstanceOf(NoAutenticadoException.class)
                .satisfies(ex -> assertThat(((NoAutenticadoException) ex).getCodigo()).isEqualTo("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void cuentaNoActivaEsRechazadaConCuentaNoVerificada() {
        Usuario usuario = usuarioLocalActivo();
        usuario.setEstadoCuenta(EstadoCuenta.PENDIENTE_VERIFICACION);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD, usuario.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> loginService.login(new LoginRequest(CORREO, PASSWORD)))
                .isInstanceOf(AccesoDenegadoException.class)
                .satisfies(ex -> assertThat(((AccesoDenegadoException) ex).getCodigo()).isEqualTo("CUENTA_INACTIVA"));
    }

    @Test
    void loginExitosoDevuelveElTokenYElResumenDelUsuario() {
        Usuario usuario = usuarioLocalActivo();
        usuario.setNombreCompleto("Dr. Juan Pérez");
        Rol rol = mock(Rol.class);
        when(rol.getCodigo()).thenReturn(RolCodigo.ODONTOLOGO);
        usuario.setRol(rol);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD, usuario.getPasswordHash())).thenReturn(true);
        when(jwtService.generar(usuario)).thenReturn("jwt-simulado");

        LoginResponse respuesta = loginService.login(new LoginRequest(CORREO, PASSWORD));

        assertThat(respuesta.token()).isEqualTo("jwt-simulado");
        assertThat(respuesta.usuario().nombreCompleto()).isEqualTo("Dr. Juan Pérez");
        assertThat(respuesta.usuario().rol()).isEqualTo("ODONTOLOGO");
    }

    private Usuario usuarioLocalActivo() {
        Usuario usuario = new Usuario();
        usuario.setPasswordHash("hash-bcrypt");
        usuario.setEstadoCuenta(EstadoCuenta.ACTIVA);
        return usuario;
    }
}
