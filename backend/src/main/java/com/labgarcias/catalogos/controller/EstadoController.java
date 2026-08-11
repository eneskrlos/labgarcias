package com.labgarcias.catalogos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.catalogos.dto.EstadoResponse;
import com.labgarcias.catalogos.service.EstadoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/estados")
@Tag(name = "Catálogos")
public class EstadoController {

    private final EstadoService estadoService;

    public EstadoController(EstadoService estadoService) {
        this.estadoService = estadoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Listar los estados del flujo de una orden (RN-04, CU-22)",
            description = "Catálogo ordenado por orden_secuencia; alimenta la línea de tiempo y el flujo lineal."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Listado de estados") })
    public ResponseEntity<List<EstadoResponse>> listar() {
        return ResponseEntity.ok(estadoService.listar());
    }
}
