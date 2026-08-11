package com.labgarcias.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.ProveedorAuth;
import com.labgarcias.seguridad.domain.Rol;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.dto.LoginResponse;
import com.labgarcias.seguridad.repository.RolRepository;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.excepcion.ConflictoException;
import com.labgarcias.shared.excepcion.NoAutenticadoException;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock
    private GoogleIdTokenVerifier verificador;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private JwtService jwtService;

    private GoogleAuthService googleAuthService;

    @BeforeEach
    void crearServicio() {
        googleAuthService = new GoogleAuthService(verificador, usuarioRepository, rolRepository, jwtService);
    }

    private Rol rolOdontologoMock() {
        Rol rol = mock(Rol.class);
        when(rol.getCodigo()).thenReturn(RolCodigo.ODONTOLOGO);
        return rol;
    }

    private GoogleIdToken.Payload payloadDePrueba(String subject, String correo, String nombre) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject(subject);
        payload.setEmail(correo);
        if (nombre != null) {
            payload.set("name", nombre);
        }
        return payload;
    }

    @Test
    void tokenInvalidoLanzaGoogleTokenInvalido() throws Exception {
        when(verificador.verify("token-malo")).thenReturn(null);

        assertThatThrownBy(() -> googleAuthService.autenticar("token-malo"))
                .isInstanceOf(NoAutenticadoException.class)
                .satisfies(ex -> assertThat(((NoAutenticadoException) ex).getCodigo()).isEqualTo("GOOGLE_TOKEN_INVALIDO"));
    }

    @Test
    void errorDeSeguridadAlVerificarLanzaGoogleTokenInvalido() throws Exception {
        when(verificador.verify("token-x")).thenThrow(new GeneralSecurityException("fallo de firma"));

        assertThatThrownBy(() -> googleAuthService.autenticar("token-x"))
                .isInstanceOf(NoAutenticadoException.class)
                .satisfies(ex -> assertThat(((NoAutenticadoException) ex).getCodigo()).isEqualTo("GOOGLE_TOKEN_INVALIDO"));
    }

    @Test
    void tokenMalformadoLanzaGoogleTokenInvalido() throws Exception {
        when(verificador.verify("no-es-un-jwt")).thenThrow(new IllegalArgumentException("formato inválido"));

        assertThatThrownBy(() -> googleAuthService.autenticar("no-es-un-jwt"))
                .isInstanceOf(NoAutenticadoException.class)
                .satisfies(ex -> assertThat(((NoAutenticadoException) ex).getCodigo()).isEqualTo("GOOGLE_TOKEN_INVALIDO"));
    }

    @Test
    void errorDeIOAlVerificarLanzaGoogleTokenInvalido() throws Exception {
        when(verificador.verify("token-y")).thenThrow(new IOException("timeout"));

        assertThatThrownBy(() -> googleAuthService.autenticar("token-y"))
                .isInstanceOf(NoAutenticadoException.class)
                .satisfies(ex -> assertThat(((NoAutenticadoException) ex).getCodigo()).isEqualTo("GOOGLE_TOKEN_INVALIDO"));
    }

    @Test
    void usuarioGoogleExistenteHaceLoginSinCrearUnoNuevo() throws Exception {
        GoogleIdToken.Payload payload = payloadDePrueba("sub-123", "juan@mail.com", "Juan");
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        when(idToken.getPayload()).thenReturn(payload);
        when(verificador.verify("token-ok")).thenReturn(idToken);

        Usuario existente = new Usuario();
        Rol rol = mock(Rol.class);
        when(rol.getCodigo()).thenReturn(RolCodigo.ODONTOLOGO);
        existente.setRol(rol);
        when(usuarioRepository.findByGoogleSubjectId("sub-123")).thenReturn(Optional.of(existente));
        when(jwtService.generar(existente)).thenReturn("jwt-google");

        LoginResponse respuesta = googleAuthService.autenticar("token-ok");

        assertThat(respuesta.token()).isEqualTo("jwt-google");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void correoYaRegistradoComoLocalNoSeDuplica() throws Exception {
        GoogleIdToken.Payload payload = payloadDePrueba("sub-nuevo", "existente@mail.com", "Nuevo");
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        when(idToken.getPayload()).thenReturn(payload);
        when(verificador.verify("token-nuevo")).thenReturn(idToken);
        when(usuarioRepository.findByGoogleSubjectId("sub-nuevo")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreoIgnoreCase("existente@mail.com")).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> googleAuthService.autenticar("token-nuevo"))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigo()).isEqualTo("CORREO_YA_REGISTRADO"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void primerLoginConGoogleCreaLaCuentaSegunRN16() throws Exception {
        GoogleIdToken.Payload payload = payloadDePrueba("sub-abc", "maria.lopez@mail.com", "María López");
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        when(idToken.getPayload()).thenReturn(payload);
        when(verificador.verify("token-abc")).thenReturn(idToken);
        when(usuarioRepository.findByGoogleSubjectId("sub-abc")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreoIgnoreCase("maria.lopez@mail.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByNombreUsuario("marialopez")).thenReturn(Optional.empty());

        Rol rolOdontologo = mock(Rol.class);
        when(rolOdontologo.getCodigo()).thenReturn(RolCodigo.ODONTOLOGO);
        when(rolRepository.findByCodigo(RolCodigo.ODONTOLOGO)).thenReturn(Optional.of(rolOdontologo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generar(any(Usuario.class))).thenReturn("jwt-nuevo");

        googleAuthService.autenticar("token-abc");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario creado = captor.getValue();

        assertThat(creado.getNombreCompleto()).isEqualTo("María López");
        assertThat(creado.getCorreo()).isEqualTo("maria.lopez@mail.com");
        assertThat(creado.getNombreUsuario()).isEqualTo("marialopez");
        assertThat(creado.getDireccion()).isNull();
        assertThat(creado.getPasswordHash()).isNull();
        assertThat(creado.getProveedorAuth()).isEqualTo(ProveedorAuth.GOOGLE);
        assertThat(creado.getGoogleSubjectId()).isEqualTo("sub-abc");
        assertThat(creado.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(creado.isCorreoVerificado()).isTrue();
    }

    @Test
    void nombreUsuarioConColisionAgregaSufijoNumerico() throws Exception {
        GoogleIdToken.Payload payload = payloadDePrueba("sub-xyz", "pedro@mail.com", "Pedro");
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        when(idToken.getPayload()).thenReturn(payload);
        when(verificador.verify("token-xyz")).thenReturn(idToken);
        when(usuarioRepository.findByGoogleSubjectId("sub-xyz")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreoIgnoreCase("pedro@mail.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByNombreUsuario("pedro")).thenReturn(Optional.of(new Usuario()));
        when(usuarioRepository.findByNombreUsuario("pedro2")).thenReturn(Optional.empty());
        Rol rolOdontologo = rolOdontologoMock();
        when(rolRepository.findByCodigo(RolCodigo.ODONTOLOGO)).thenReturn(Optional.of(rolOdontologo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generar(any(Usuario.class))).thenReturn("jwt");

        googleAuthService.autenticar("token-xyz");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getNombreUsuario()).isEqualTo("pedro2");
    }

    @Test
    void sinClaimNameElNombreCompletoUsaElCorreoComoRespaldo() throws Exception {
        GoogleIdToken.Payload payload = payloadDePrueba("sub-sin-nombre", "sinnombre@mail.com", null);
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        when(idToken.getPayload()).thenReturn(payload);
        when(verificador.verify("token-sin-nombre")).thenReturn(idToken);
        when(usuarioRepository.findByGoogleSubjectId("sub-sin-nombre")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreoIgnoreCase("sinnombre@mail.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByNombreUsuario("sinnombre")).thenReturn(Optional.empty());
        Rol rolOdontologo = rolOdontologoMock();
        when(rolRepository.findByCodigo(RolCodigo.ODONTOLOGO)).thenReturn(Optional.of(rolOdontologo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generar(any(Usuario.class))).thenReturn("jwt");

        googleAuthService.autenticar("token-sin-nombre");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getNombreCompleto()).isEqualTo("sinnombre@mail.com");
    }
}
