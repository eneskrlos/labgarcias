package com.labgarcias.catalogos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.catalogos.domain.Estado;

public interface EstadoRepository extends JpaRepository<Estado, Short> {

    /** RN-04: el flujo lineal se recorre en orden de secuencia. */
    List<Estado> findAllByOrderByOrdenSecuenciaAsc();
}
