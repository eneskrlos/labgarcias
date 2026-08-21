package com.labgarcias.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.labgarcias.seguridad.domain.Rol;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.dto.CambiarPasswordRequest;
import com.labgarcias.seguridad.dto.LoginResponse;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.excepcion.DominioException;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;
import com.labgarcias.shared.excepcion.ReglaNegocioException;
import com.labgarcias.shared.excepcion.ValidacionException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordServiceTest {

    private static final long USUARIO = 7L;
    private static final String TEMPORAL = "Ab3$Kd9!Xz2P";
    private static final String NUEVA = "MiClave2026$";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private PasswordService passwordService;

    private Usuario odontologo;

    @BeforeEach
    void prepararOdontologoConCambioPendiente() {
        Rol rol = mock(Rol.class);
        when(rol.getCodigo()).thenReturn(RolCodigo.ODONTOLOGO);

        odontologo = new Usuario();
        odontologo.setRol(rol);
        odontologo.setNombreCompleto("Dr. Juan Pérez");
        odontologo.setPasswordHash("$2a$10$hashDeLaTemporal");
        odontologo.setDebeCambiarPassword(true);

        when(usuarioRepository.findById(USUARIO)).thenReturn(Optional.of(odontologo));
        when(passwordEncoder.matches(TEMPORAL, "$2a$10$hashDeLaTemporal")).thenReturn(true);
        when(passwordEncoder.encode(NUEVA)).thenReturn("$2a$10$hashDeLaNueva");
        when(jwtService.generar(odontologo)).thenReturn("token-normal");
    }

    /** §3.1.b: apaga la bandera y guarda el hash de la nueva. */
    @Test
    void cambiarApagaLaBanderaYGuardaElHashNuevo() {
        passwordService.cambiar(USUARIO, new CambiarPasswordRequest(TEMPORAL, NUEVA));

        assertThat(odontologo.isDebeCambiarPassword()).isFalse();
        assertThat(odontologo.getPasswordHash()).isEqualTo("$2a$10$hashDeLaNueva");
    }

    /** §3.1.b: "emite el token normal" — el usuario sigue trabajando sin volver a loguearse. */
    @Test
    void cambiarDevuelveUnTokenSinRestriccion() {
        LoginResponse respuesta = passwordService.cambiar(USUARIO, new CambiarPasswordRequest(TEMPORAL, NUEVA));

        assertThat(respuesta.token()).isEqualTo("token-normal");
        assertThat(respuesta.debeCambiarPassword()).isFalse();
        assertThat(respuesta.usuario().rol()).isEqualTo("ODONTOLOGO");
    }

    /** El token se emite después de apagar la bandera, si no volvería a salir restringido. */
    @Test
    void elTokenSeEmiteConLaBanderaYaApagada() {
        when(jwtService.generar(odontologo)).thenAnswer(invocacion -> {
            assertThat(odontologo.isDebeCambiarPassword()).isFalse();
            return "token-normal";
        });

        passwordService.cambiar(USUARIO, new CambiarPasswordRequest(TEMPORAL, NUEVA));

        verify(jwtService).generar(odontologo);
    }

    @Test
    void unaContrasenaActualIncorrectaNoCambiaNada() {
        when(passwordEncoder.matches("otra", "$2a$10$hashDeLaTemporal")).thenReturn(false);

        assertThatThrownBy(() -> passwordService.cambiar(USUARIO, new CambiarPasswordRequest("otra", NUEVA)))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("PASSWORD_ACTUAL_INCORRECTA");

        assertThat(odontologo.isDebeCambiarPassword()).isTrue();
        verify(passwordEncoder, never()).encode(anyString());
    }

    /** RN-15 también rige para la contraseña que elige el usuario. */
    @Test
    void rn15SeValidaSobreLaContrasenaNueva() {
        assertThatThrownBy(() -> passwordService.cambiar(USUARIO, new CambiarPasswordRequest(TEMPORAL, "corta")))
                .isInstanceOf(ValidacionException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("PASSWORD_INVALIDA");

        assertThat(odontologo.isDebeCambiarPassword()).isTrue();
    }

    @Test
    void unUsuarioInexistenteResponde404() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordService.cambiar(99L, new CambiarPasswordRequest(TEMPORAL, NUEVA)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
