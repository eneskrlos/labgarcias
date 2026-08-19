package com.labgarcias.seguridad.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.repository.UsuarioRepository;
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
}
