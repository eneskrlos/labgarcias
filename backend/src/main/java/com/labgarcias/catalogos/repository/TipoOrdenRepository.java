package com.labgarcias.catalogos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.catalogos.domain.TipoOrden;

public interface TipoOrdenRepository extends JpaRepository<TipoOrden, Short> {
}
