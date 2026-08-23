package com.labgarcias.seguridad.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.dto.LoginRequest;
import com.labgarcias.seguridad.dto.LoginResponse;
import com.labgarcias.seguridad.dto.UsuarioResumenResponse;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.excepcion.AccesoDenegadoException;
import com.labgarcias.shared.excepcion.NoAutenticadoException;

@Service
public class LoginService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(request.correo()).orElse(null);

        if (!credencialesValidas(usuario, request.password())) {
            throw new NoAutenticadoException("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos.");
        }
        if (usuario.getEstadoCuenta() != EstadoCuenta.ACTIVA) {
            // D-18: las cuentas nacen ACTIVA (las crea el admin); el único caso real es una cuenta dada de baja.
            throw new AccesoDenegadoException("CUENTA_INACTIVA", "Tu cuenta está inactiva. Contactá al laboratorio.");
        }

        String token = jwtService.generar(usuario);
        UsuarioResumenResponse resumen = new UsuarioResumenResponse(
                usuario.getId(), usuario.getNombreCompleto(), usuario.getRol().getCodigo().name());
        // §3.1.b: la bandera viaja en la respuesta para el frontend y dentro del token para el
        // backend. La que manda es la del token: el cliente no puede quitársela.
        return new LoginResponse(token, usuario.isDebeCambiarPassword(), resumen);
    }

    /** RN-16: un usuario GOOGLE no tiene passwordHash; nunca puede autenticarse por este medio. */
    private boolean credencialesValidas(Usuario usuario, String password) {
        return usuario != null
                && usuario.getPasswordHash() != null
                && passwordEncoder.matches(password, usuario.getPasswordHash());
    }
}
