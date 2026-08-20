package com.labgarcias.seguridad.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** §6.2: los destinatarios de las notificaciones dirigidas al laboratorio. */
    List<Usuario> findByRolCodigoInAndEstadoCuenta(Collection<RolCodigo> codigos, EstadoCuenta estadoCuenta);

    /** CU-01: correo es único sin distinguir mayúsculas/minúsculas (idx_usuario_correo). */
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    /** D-18: nombreUsuario debe ser único al dar de alta la cuenta. */
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
}
