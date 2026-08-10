package com.labgarcias.seguridad.service;

import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.labgarcias.shared.util.ConstantesDominio;

@Service
public class RegistroService {

    private static final Pattern MAYUSCULA = Pattern.compile("[A-Z]");
    private static final Pattern MINUSCULA = Pattern.compile("[a-z]");
    private static final Pattern NUMERO = Pattern.compile("[0-9]");
    private static final Pattern CARACTER_ESPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final VerificacionService verificacionService;
    private final PasswordEncoder passwordEncoder;

    public RegistroService(UsuarioRepository usuarioRepository,
                            RolRepository rolRepository,
                            VerificacionService verificacionService,
                            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.verificacionService = verificacionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registrarOdontologo(RegistroOdontologoRequest request) {
        validarPasswordSegura(request.password());
        validarCorreoDisponible(request.correo());
        validarNombreUsuarioDisponible(request.nombreUsuario());

        Usuario usuario = crearUsuario(request);
        usuarioRepository.save(usuario);
        verificacionService.generarToken(usuario);
    }

    private Usuario crearUsuario(RegistroOdontologoRequest request) {
        Rol rolOdontologo = rolRepository.findByCodigo(RolCodigo.ODONTOLOGO)
                .orElseThrow(() -> new IllegalStateException("Rol ODONTOLOGO no está cargado en el catálogo."));

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(request.nombreCompleto());
        usuario.setCorreo(request.correo());
        usuario.setNombreUsuario(request.nombreUsuario());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setDireccion(request.direccion());
        usuario.setRol(rolOdontologo);
        usuario.setProveedorAuth(ProveedorAuth.LOCAL);
        usuario.setEstadoCuenta(EstadoCuenta.PENDIENTE_VERIFICACION);
        usuario.setCorreoVerificado(false);
        return usuario;
    }

    private void validarPasswordSegura(String password) {
        boolean cumple = password.length() >= ConstantesDominio.LONGITUD_MINIMA_PASSWORD
                && MAYUSCULA.matcher(password).find()
                && MINUSCULA.matcher(password).find()
                && NUMERO.matcher(password).find()
                && CARACTER_ESPECIAL.matcher(password).find();
        if (!cumple) {
            throw new ValidacionException("PASSWORD_INVALIDA",
                    "La contraseña debe tener al menos " + ConstantesDominio.LONGITUD_MINIMA_PASSWORD
                            + " caracteres, con al menos una mayúscula, una minúscula, un número y un carácter especial.",
                    "password");
        }
    }

    private void validarCorreoDisponible(String correo) {
        if (usuarioRepository.findByCorreoIgnoreCase(correo).isPresent()) {
            throw new ConflictoException("CORREO_YA_REGISTRADO", "Ya existe una cuenta con ese correo.", "correo");
        }
    }

    private void validarNombreUsuarioDisponible(String nombreUsuario) {
        if (usuarioRepository.findByNombreUsuario(nombreUsuario).isPresent()) {
            throw new ConflictoException("USUARIO_YA_REGISTRADO", "Ese nombre de usuario ya está en uso.", "nombreUsuario");
        }
    }
}
