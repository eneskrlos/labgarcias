package com.labgarcias.seguridad.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.seguridad.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** CU-01/CU-18: correo es único sin distinguir mayúsculas/minúsculas (idx_usuario_correo). */
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    /** CU-18: nombreUsuario debe ser único. */
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    /** RN-16: login/registro con Google identifica al usuario por el subject id del proveedor. */
    Optional<Usuario> findByGoogleSubjectId(String googleSubjectId);
}
