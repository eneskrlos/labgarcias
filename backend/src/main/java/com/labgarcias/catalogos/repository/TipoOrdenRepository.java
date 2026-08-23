package com.labgarcias.catalogos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.catalogos.domain.CodigoTipoOrden;
import com.labgarcias.catalogos.domain.TipoOrden;

public interface TipoOrdenRepository extends JpaRepository<TipoOrden, Short> {

    /** RN-11/CU-09: la orden llega con el código y de ahí salen estado inicial y recargo. */
    Optional<TipoOrden> findByCodigo(CodigoTipoOrden codigo);
}
