package com.labgarcias.seguridad.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.seguridad.domain.CredencialesCreadasEvent;
import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.ProveedorAuth;
import com.labgarcias.seguridad.domain.Rol;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.dto.CrearOdontologoRequest;
import com.labgarcias.seguridad.dto.OdontologoActivoResponse;
import com.labgarcias.seguridad.dto.OdontologoResponse;
import com.labgarcias.seguridad.repository.RolRepository;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.dto.PaginaResponse;
import com.labgarcias.shared.excepcion.ConflictoException;
import com.labgarcias.shared.excepcion.ReglaNegocioException;
import com.labgarcias.shared.util.ValidadorPaginacion;

/**
 * D-18/§3.1.b: el administrador crea la cuenta del odontólogo.
 *
 * La contraseña se genera acá, se guarda **solo como hash** y sale del método únicamente dentro
 * del evento, en memoria. Ni la respuesta ni la base ni los logs la ven.
 */
@Service
public class OdontologoService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SolicitudAccesoService solicitudAccesoService;
    private final GeneradorPassword generadorPassword;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher publicadorEventos;

    public OdontologoService(UsuarioRepository usuarioRepository,
                             RolRepository rolRepository,
                             SolicitudAccesoService solicitudAccesoService,
                             GeneradorPassword generadorPassword,
                             PasswordEncoder passwordEncoder,
                             ApplicationEventPublisher publicadorEventos) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.solicitudAccesoService = solicitudAccesoService;
        this.generadorPassword = generadorPassword;
        this.passwordEncoder = passwordEncoder;
        this.publicadorEventos = publicadorEventos;
    }

    @Transactional
    public OdontologoResponse crear(CrearOdontologoRequest request) {
        validarCorreoDisponible(request.correo());
        validarNombreUsuarioDisponible(request.nombreUsuario());

        String passwordTemporal = generadorPassword.generar();
        Usuario odontologo = usuarioRepository.save(nuevoOdontologo(request, passwordTemporal));

        if (request.solicitudId() != null) {
            solicitudAccesoService.aprobar(request.solicitudId());
        }

        // §3.1.b paso 4: el listener corre AFTER_COMMIT y es el único que ve la contraseña.
        publicadorEventos.publishEvent(new CredencialesCreadasEvent(
                odontologo.getId(), odontologo.getCorreo(), odontologo.getNombreUsuario(), passwordTemporal));

        return OdontologoResponse.de(odontologo);
    }

    /**
     * §5.1/D-19: los odontólogos que el laboratorio puede elegir al registrar una orden.
     *
     * **Sin paginar**, por el mismo motivo que `/tipos-trabajo/activos`: alimenta un selector que
     * necesita la lista entera. Solo cuentas ACTIVAS, que son las únicas que `POST /ordenes`
     * acepta como dueño; ofrecer una dada de baja sería ofrecer un alta que va a fallar.
     */
    @Transactional(readOnly = true)
    public List<OdontologoActivoResponse> listarActivos() {
        return usuarioRepository
                .findByRolCodigoAndEstadoCuentaOrderByNombreCompletoAsc(RolCodigo.ODONTOLOGO, EstadoCuenta.ACTIVA)
                .stream()
                .map(OdontologoActivoResponse::de)
                .toList();
    }

    /**
     * CU-11/§7: la tabla administrable de odontólogos, **paginada** (§8.1 Regla 2).
     *
     * **Es otra cosa que `listarActivos`** y conviven: aquella alimenta el selector de §5.1 y
     * devuelve solo `id` y `nombreCompleto` de las cuentas ACTIVA; esta es la tabla de CU-11, con
     * los datos de cada cuenta y **también las dadas de baja**, que es donde se las vuelve a
     * activar. Devolver solo las activas dejaría una cuenta inactiva fuera de toda pantalla.
     */
    @Transactional(readOnly = true)
    public PaginaResponse<OdontologoResponse> listar(Pageable pageable) {
        ValidadorPaginacion.validarTamano(pageable.getPageSize());
        return PaginaResponse.de(usuarioRepository
                .findByRolCodigoOrderByNombreCompletoAsc(RolCodigo.ODONTOLOGO, pageable)
                .map(OdontologoResponse::de));
    }

    /**
     * §3.1.b paso 3: rol ODONTOLOGO, cuenta ACTIVA y correo dado por verificado —el alta la hizo
     * el administrador, que es la verificación (D-18)— y la bandera de cambio obligatorio encendida.
     */
    private Usuario nuevoOdontologo(CrearOdontologoRequest request, String passwordTemporal) {
        Usuario odontologo = new Usuario();
        odontologo.setRol(rolOdontologo());
        odontologo.setNombreCompleto(request.nombreCompleto());
        odontologo.setCorreo(request.correo());
        odontologo.setNombreUsuario(request.nombreUsuario());
        odontologo.setDireccion(textoONulo(request.direccion()));
        odontologo.setTelefono(textoONulo(request.telefono()));
        odontologo.setPasswordHash(passwordEncoder.encode(passwordTemporal));
        odontologo.setProveedorAuth(ProveedorAuth.LOCAL);
        odontologo.setEstadoCuenta(EstadoCuenta.ACTIVA);
        odontologo.setCorreoVerificado(true);
        odontologo.setDebeCambiarPassword(true);
        return odontologo;
    }

    private Rol rolOdontologo() {
        return rolRepository.findByCodigo(RolCodigo.ODONTOLOGO)
                .orElseThrow(() -> new ReglaNegocioException("ROL_NO_DISPONIBLE",
                        "No está configurado el rol de odontólogo."));
    }

    private String textoONulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    private void validarCorreoDisponible(String correo) {
        if (usuarioRepository.findByCorreoIgnoreCase(correo).isPresent()) {
            throw new ConflictoException("CORREO_YA_REGISTRADO",
                    "Ya existe una cuenta con ese correo.", "correo");
        }
    }

    private void validarNombreUsuarioDisponible(String nombreUsuario) {
        if (usuarioRepository.findByNombreUsuario(nombreUsuario).isPresent()) {
            throw new ConflictoException("NOMBRE_USUARIO_YA_REGISTRADO",
                    "Ya existe una cuenta con ese nombre de usuario.", "nombreUsuario");
        }
    }
}
