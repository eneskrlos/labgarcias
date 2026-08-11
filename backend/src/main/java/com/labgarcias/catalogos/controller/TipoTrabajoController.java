package com.labgarcias.catalogos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.catalogos.dto.TipoTrabajoResponse;
import com.labgarcias.catalogos.service.TipoTrabajoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/tipos-trabajo")
@Tag(name = "Catálogos")
public class TipoTrabajoController {

    private final TipoTrabajoService tipoTrabajoService;

    public TipoTrabajoController(TipoTrabajoService tipoTrabajoService) {
        this.tipoTrabajoService = tipoTrabajoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Listar tipos de trabajo activos (CU-16, CU-09)",
            description = "Catálogo que puede elegir el odontólogo al crear una orden. Los tipos desactivados no aparecen."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Listado de tipos activos") })
    public ResponseEntity<List<TipoTrabajoResponse>> listarActivos() {
        return ResponseEntity.ok(tipoTrabajoService.listarActivos());
    }
}
