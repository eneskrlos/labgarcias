package com.labgarcias.seguridad.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.seguridad.domain.TokenVerificacion;

public interface TokenVerificacionRepository extends JpaRepository<TokenVerificacion, Long> {

    /** CU-19: valida el enlace recibido por correo. */
    Optional<TokenVerificacion> findByToken(String token);

    /** CU-19 A1 (reenvío): tokens todavía no usados del usuario, para invalidarlos. */
    List<TokenVerificacion> findByUsuarioIdAndFechaUsoIsNull(Long usuarioId);
}
