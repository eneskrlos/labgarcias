package com.labgarcias.ordenes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labgarcias.ordenes.domain.Orden;

public interface OrdenRepository extends JpaRepository<Orden, Long> {
}
