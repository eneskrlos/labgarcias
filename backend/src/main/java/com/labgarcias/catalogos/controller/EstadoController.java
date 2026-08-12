package com.labgarcias.catalogos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.catalogos.dto.EstadoActualizarRequest;
import com.labgarcias.catalogos.dto.EstadoResponse;
import com.labgarcias.catalogos.service.EstadoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Editar nombre y descripción de un estado (CU-22)",
            description = "RN-04: codigo, orden_secuencia, es_terminal y es_productivo son fijos porque el flujo lineal "
                    + "depende de ellos; no son editables ni por esta vía. No hay alta ni baja de estados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "404", description = "ESTADO_NO_ENCONTRADO")
    })
    public ResponseEntity<EstadoResponse> actualizar(@PathVariable Short id, @Valid @RequestBody EstadoActualizarRequest request) {
        return ResponseEntity.ok(estadoService.actualizar(id, request));
    }
}
