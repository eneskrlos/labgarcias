package com.labgarcias.seguridad.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.seguridad.domain.Rol;
import com.labgarcias.seguridad.domain.RolCodigo;

public interface RolRepository extends JpaRepository<Rol, Short> {

    Optional<Rol> findByCodigo(RolCodigo codigo);
}
