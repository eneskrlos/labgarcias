package com.labgarcias.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.ProveedorAuth;
import com.labgarcias.seguridad.domain.Rol;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.dto.RegistroOdontologoRequest;
import com.labgarcias.seguridad.repository.RolRepository;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.excepcion.ConflictoException;
import com.labgarcias.shared.excepcion.ValidacionException;

@ExtendWith(MockitoExtension.class)
class RegistroServiceTest {

    private static final String PASSWORD_VALIDA = "*38Op5)l6";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private VerificacionService verificacionService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistroService registroService;

    private RegistroOdontologoRequest requestValido;

    @BeforeEach
    void configurarRequestValido() {
        requestValido = new RegistroOdontologoRequest(
                "Dr. Juan Pérez", "juan@mail.com", "jperez", PASSWORD_VALIDA, "Av. Siempre Viva 123");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "corta1!A",       // menos de 9 caracteres
            "sinmayuscula1!", // sin mayúscula
            "SINMINUSCULA1!", // sin minúscula
            "SinNumeroAqui!", // sin número
            "SinCaracterEsp1" // sin carácter especial
    })
    void rechazaPasswordQueNoCumpleRN15(String passwordInvalida) {
        RegistroOdontologoRequest request = new RegistroOdontologoRequest(
                "Dr. Juan Pérez", "juan@mail.com", "jperez", passwordInvalida, "Dirección");

        assertThatThrownBy(() -> registroService.registrarOdontologo(request))
                .isInstanceOf(ValidacionException.class)
                .satisfies(ex -> assertThat(((ValidacionException) ex).getCodigo()).isEqualTo("PASSWORD_INVALIDA"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaCorreoYaRegistrado() {
        when(usuarioRepository.findByCorreoIgnoreCase("juan@mail.com"))
                .thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> registroService.registrarOdontologo(requestValido))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigo()).isEqualTo("CORREO_YA_REGISTRADO"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaNombreUsuarioYaRegistrado() {
        when(usuarioRepository.findByCorreoIgnoreCase("juan@mail.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByNombreUsuario("jperez")).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> registroService.registrarOdontologo(requestValido))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigo()).isEqualTo("USUARIO_YA_REGISTRADO"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registraElUsuarioConLosCamposCU18YGeneraElTokenDeVerificacion() {
        Rol rolOdontologo = mock(Rol.class);
        when(usuarioRepository.findByCorreoIgnoreCase("juan@mail.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByNombreUsuario("jperez")).thenReturn(Optional.empty());
        when(rolRepository.findByCodigo(RolCodigo.ODONTOLOGO)).thenReturn(Optional.of(rolOdontologo));
        when(passwordEncoder.encode(PASSWORD_VALIDA)).thenReturn("hash-bcrypt-simulado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        registroService.registrarOdontologo(requestValido);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();

        assertThat(guardado.getNombreCompleto()).isEqualTo("Dr. Juan Pérez");
        assertThat(guardado.getCorreo()).isEqualTo("juan@mail.com");
        assertThat(guardado.getNombreUsuario()).isEqualTo("jperez");
        assertThat(guardado.getPasswordHash()).isEqualTo("hash-bcrypt-simulado");
        assertThat(guardado.getRol()).isSameAs(rolOdontologo);
        assertThat(guardado.getProveedorAuth()).isEqualTo(ProveedorAuth.LOCAL);
        assertThat(guardado.getEstadoCuenta()).isEqualTo(EstadoCuenta.PENDIENTE_VERIFICACION);
        assertThat(guardado.isCorreoVerificado()).isFalse();

        verify(verificacionService, times(1)).generarToken(guardado);
    }

    @Test
    void laContrasenaNuncaSeGuardaEnTextoPlano() {
        when(usuarioRepository.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByNombreUsuario(anyString())).thenReturn(Optional.empty());
        when(rolRepository.findByCodigo(RolCodigo.ODONTOLOGO)).thenReturn(Optional.of(mock(Rol.class)));
        when(passwordEncoder.encode(PASSWORD_VALIDA)).thenReturn("$2a$10$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        registroService.registrarOdontologo(requestValido);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo(PASSWORD_VALIDA);
    }

    @Test
    void siElRolOdontologoNoEstaCargadoFallaExplicitamente() {
        when(usuarioRepository.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByNombreUsuario(anyString())).thenReturn(Optional.empty());
        when(rolRepository.findByCodigo(RolCodigo.ODONTOLOGO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registroService.registrarOdontologo(requestValido))
                .isInstanceOf(IllegalStateException.class);
    }
}
