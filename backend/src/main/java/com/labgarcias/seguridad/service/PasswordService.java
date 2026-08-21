package com.labgarcias.seguridad.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.dto.CambiarPasswordRequest;
import com.labgarcias.seguridad.dto.LoginResponse;
import com.labgarcias.seguridad.dto.UsuarioResumenResponse;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;
import com.labgarcias.shared.excepcion.ReglaNegocioException;

/**
 * §3.1.b: el cambio obligatorio del primer ingreso.
 *
 * Al terminar emite el token normal, sin la restricción: es lo que devuelve el acceso al resto
 * del sistema sin obligar a volver a iniciar sesión.
 */
@Service
public class PasswordService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public PasswordService(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse cambiar(Long usuarioId, CambiarPasswordRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO",
                        "El usuario no existe."));

        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            // 422 y no 401: la sesión es válida, lo que no coincide es el dato que mandó el usuario.
            // Un 401 haría que el cliente lo interpretara como sesión vencida y lo expulsara.
            throw new ReglaNegocioException("PASSWORD_ACTUAL_INCORRECTA",
                    "La contraseña actual no es correcta.", "passwordActual");
        }

        ValidadorPassword.validar(request.passwordNueva(), "passwordNueva");

        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        usuario.setDebeCambiarPassword(false);

        return new LoginResponse(jwtService.generar(usuario), false, resumenDe(usuario));
    }

    private UsuarioResumenResponse resumenDe(Usuario usuario) {
        return new UsuarioResumenResponse(
                usuario.getId(), usuario.getNombreCompleto(), usuario.getRol().getCodigo().name());
    }
}
