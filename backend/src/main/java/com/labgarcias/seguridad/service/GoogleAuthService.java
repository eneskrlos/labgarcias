package com.labgarcias.seguridad.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.ProveedorAuth;
import com.labgarcias.seguridad.domain.Rol;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.dto.LoginResponse;
import com.labgarcias.seguridad.dto.UsuarioResumenResponse;
import com.labgarcias.seguridad.repository.RolRepository;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.excepcion.ConflictoException;
import com.labgarcias.shared.excepcion.NoAutenticadoException;

/** RN-16/CU-19 A2: registro y login con Google. */
@Service
public class GoogleAuthService {

    private final GoogleIdTokenVerifier verificador;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final JwtService jwtService;

    public GoogleAuthService(@Value("${app.google.client-id}") String googleClientId,
                              UsuarioRepository usuarioRepository,
                              RolRepository rolRepository,
                              JwtService jwtService) {
        this.verificador = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse autenticar(String idTokenValor) {
        GoogleIdToken.Payload payload = verificarToken(idTokenValor);

        Usuario usuario = usuarioRepository.findByGoogleSubjectId(payload.getSubject())
                .orElseGet(() -> registrarUsuarioGoogle(payload));

        String token = jwtService.generar(usuario);
        UsuarioResumenResponse resumen = new UsuarioResumenResponse(
                usuario.getId(), usuario.getNombreCompleto(), usuario.getRol().getCodigo().name());
        return new LoginResponse(token, resumen);
    }

    private GoogleIdToken.Payload verificarToken(String idTokenValor) {
        try {
            GoogleIdToken idToken = verificador.verify(idTokenValor);
            if (idToken == null) {
                throw new NoAutenticadoException("GOOGLE_TOKEN_INVALIDO", "El token de Google no es válido.");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException | IllegalArgumentException excepcion) {
            // Un idToken malformado (no es un JWT) llega como IllegalArgumentException, no como
            // GeneralSecurityException/IOException: la librería de Google no distingue "inválido"
            // de "malformado" en su jerarquía de excepciones.
            throw new NoAutenticadoException("GOOGLE_TOKEN_INVALIDO", "El token de Google no es válido.");
        }
    }

    /** RN-16: un correo ya registrado como LOCAL no se duplica. */
    private Usuario registrarUsuarioGoogle(GoogleIdToken.Payload payload) {
        String correo = payload.getEmail();
        if (usuarioRepository.findByCorreoIgnoreCase(correo).isPresent()) {
            throw new ConflictoException("CORREO_YA_REGISTRADO", "Ya existe una cuenta con ese correo.", "correo");
        }

        Rol rolOdontologo = rolRepository.findByCodigo(RolCodigo.ODONTOLOGO)
                .orElseThrow(() -> new IllegalStateException("Rol ODONTOLOGO no está cargado en el catálogo."));

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(nombreCompletoDe(payload, correo));
        usuario.setCorreo(correo);
        usuario.setNombreUsuario(generarNombreUsuarioDisponible(correo));
        usuario.setDireccion(null);
        usuario.setRol(rolOdontologo);
        usuario.setProveedorAuth(ProveedorAuth.GOOGLE);
        usuario.setGoogleSubjectId(payload.getSubject());
        usuario.setEstadoCuenta(EstadoCuenta.ACTIVA);
        usuario.setCorreoVerificado(true);
        return usuarioRepository.save(usuario);
    }

    private String nombreCompletoDe(GoogleIdToken.Payload payload, String correo) {
        Object nombre = payload.get("name");
        return nombre != null ? nombre.toString() : correo;
    }

    /** Deriva nombreUsuario del prefijo del correo; agrega un sufijo numérico si ya está tomado. */
    private String generarNombreUsuarioDisponible(String correo) {
        String base = correo.substring(0, correo.indexOf('@')).replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String candidato = base;
        int sufijo = 2;
        while (usuarioRepository.findByNombreUsuario(candidato).isPresent()) {
            candidato = base + sufijo;
            sufijo++;
        }
        return candidato;
    }
}
