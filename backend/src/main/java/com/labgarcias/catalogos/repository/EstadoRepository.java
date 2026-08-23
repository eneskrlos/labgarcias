package com.labgarcias.catalogos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.catalogos.domain.Estado;

public interface EstadoRepository extends JpaRepository<Estado, Short> {

    /** RN-04: el flujo lineal se recorre en orden de secuencia. */
    List<Estado> findAllByOrderByOrdenSecuenciaAsc();

    /** CU-06/CU-20: el estado destino de una transición llega identificado por su código. */
    Optional<Estado> findByCodigo(String codigo);
}
