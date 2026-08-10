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
            throw new AccesoDenegadoException("CUENTA_NO_VERIFICADA", "Todavía no verificaste tu cuenta por correo.");
        }

        String token = jwtService.generar(usuario);
        UsuarioResumenResponse resumen = new UsuarioResumenResponse(
                usuario.getId(), usuario.getNombreCompleto(), usuario.getRol().getCodigo().name());
        return new LoginResponse(token, resumen);
    }

    /** RN-16: un usuario GOOGLE no tiene passwordHash; nunca puede autenticarse por este medio. */
    private boolean credencialesValidas(Usuario usuario, String password) {
        return usuario != null
                && usuario.getPasswordHash() != null
                && passwordEncoder.matches(password, usuario.getPasswordHash());
    }
}
