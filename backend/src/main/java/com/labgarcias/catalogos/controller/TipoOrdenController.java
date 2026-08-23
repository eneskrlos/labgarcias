package com.labgarcias.catalogos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.catalogos.dto.TipoOrdenResponse;
import com.labgarcias.catalogos.service.TipoOrdenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/tipos-orden")
@Tag(name = "Catálogos")
public class TipoOrdenController {

    private final TipoOrdenService tipoOrdenService;

    public TipoOrdenController(TipoOrdenService tipoOrdenService) {
        this.tipoOrdenService = tipoOrdenService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Listar tipos de orden (RN-11, CU-15)",
            description = "Solo lectura: el comportamiento diferencial Normal/Urgente es configuración del sistema, no hay endpoints de escritura."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Listado de tipos de orden") })
    public ResponseEntity<List<TipoOrdenResponse>> listar() {
        return ResponseEntity.ok(tipoOrdenService.listar());
    }
}
