package com.labgarcias.seguridad.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * CU-11/§7: la tabla administrable de odontólogos, **paginada** (§8.1 Regla 2). Trae todas las
     * cuentas de ese rol, activas e inactivas: el listado es donde se las da de baja y de alta.
     * Es otra cosa que `findByRolCodigoAndEstadoCuentaOrderByNombreCompletoAsc`, que alimenta un
     * selector y solo devuelve las ACTIVA.
     */
    Page<Usuario> findByRolCodigoOrderByNombreCompletoAsc(RolCodigo codigo, Pageable pageable);

    /** CU-17/§7: el padrón completo para el SUPERADMIN, de cualquier rol. */
    Page<Usuario> findAllByOrderByNombreCompletoAsc(Pageable pageable);

    /** §6.2: los destinatarios de las notificaciones dirigidas al laboratorio. */
    List<Usuario> findByRolCodigoInAndEstadoCuenta(Collection<RolCodigo> codigos, EstadoCuenta estadoCuenta);

    /**
     * §5.1/D-19: los odontólogos que puede elegir el laboratorio al registrar una orden. Ordenados
     * por nombre porque el destino es un selector que una persona lee.
     */
    List<Usuario> findByRolCodigoAndEstadoCuentaOrderByNombreCompletoAsc(RolCodigo codigo,
                                                                         EstadoCuenta estadoCuenta);

    /** CU-01: correo es único sin distinguir mayúsculas/minúsculas (idx_usuario_correo). */
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    /** D-18: nombreUsuario debe ser único al dar de alta la cuenta. */
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
}
