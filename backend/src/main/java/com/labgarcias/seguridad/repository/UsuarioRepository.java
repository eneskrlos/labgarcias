package com.labgarcias.seguridad.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.seguridad.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** CU-01: correo es único sin distinguir mayúsculas/minúsculas (idx_usuario_correo). */
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    /** D-18: nombreUsuario debe ser único al dar de alta la cuenta. */
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
}
