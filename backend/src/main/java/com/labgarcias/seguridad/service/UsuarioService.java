package com.labgarcias.seguridad.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.dto.PerfilResponse;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;
import com.labgarcias.shared.excepcion.ReglaNegocioException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * §5.1/D-19: el laboratorio registra la orden a nombre de un odontólogo, así que hay que
     * confirmar que el id recibido corresponde a una cuenta de odontólogo activa.
     *
     * Un id inexistente, uno de otro rol y una cuenta dada de baja se rechazan con el mismo
     * código y el mismo mensaje: distinguirlos le contaría a quien prueba ids qué cuentas
     * existen y con qué rol.
     *
     * Devuelve dominio (no DTO) porque Agente.md 5.4 prohíbe importar el dto de otro módulo.
     */
    @Transactional(readOnly = true)
    public Usuario obtenerOdontologoActivoParaOrden(Long id) {
        return usuarioRepository.findById(id)
                .filter(usuario -> usuario.getRol().getCodigo() == RolCodigo.ODONTOLOGO)
                .filter(usuario -> usuario.getEstadoCuenta() == EstadoCuenta.ACTIVA)
                .orElseThrow(() -> new ReglaNegocioException("ODONTOLOGO_INVALIDO",
                        "El odontólogo indicado no existe o no está activo.", "odontologoId"));
    }

    /**
     * §7: los datos del usuario autenticado, para el perfil.
     *
     * Devuelve dominio (no DTO) porque Agente.md 5.4 prohíbe importar el dto de otro módulo.
     */
    @Transactional(readOnly = true)
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "USUARIO_NO_ENCONTRADO", "No existe el usuario solicitado."));
    }

    /**
     * §7: el perfil del usuario autenticado. Se arma acá y no en el controller porque el rol es
     * una relación perezosa: fuera de la transacción, leerlo falla.
     */
    @Transactional(readOnly = true)
    public PerfilResponse obtenerPerfil(Long id) {
        return PerfilResponse.de(obtenerPorId(id));
    }

    /**
     * §6.5 paso 4: el chat de Telegram es una columna de `usuario`, así que la escribe su propio
     * módulo. El módulo de notificaciones, que es el que recibe el `/start` del bot, llega hasta
     * acá por servicio y nunca por el repositorio (Agente.md 5.4).
     */
    @Transactional
    public void vincularTelegram(Long id, String chatId) {
        obtenerPorId(id).vincularTelegram(chatId);
    }

    /** §6.5 paso 5. Desvincular dos veces no es un error: el estado final es el mismo. */
    @Transactional
    public void desvincularTelegram(Long id) {
        obtenerPorId(id).desvincularTelegram();
    }

    /**
     * §6.2: varias notificaciones van dirigidas "al Administrador", pero notificacion.destinatario_id
     * apunta a un usuario concreto y puede haber más de una cuenta de administración. Se le entrega
     * a cada una: la campana y los canales de RN-19 son por usuario, no del laboratorio como bloque.
     *
     * Las cuentas dadas de baja quedan afuera: seguirían acumulando avisos que nadie va a leer.
     *
     * Devuelve dominio (no DTO) porque Agente.md 5.4 prohíbe importar el dto de otro módulo.
     */
    @Transactional(readOnly = true)
    public List<Usuario> listarAdministradoresActivosParaNotificacion() {
        return usuarioRepository.findByRolCodigoInAndEstadoCuenta(
                List.of(RolCodigo.ADMIN, RolCodigo.SUPERADMIN), EstadoCuenta.ACTIVA);
    }
}
