package com.labgarcias.catalogos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.catalogos.dto.CambiarEstadoTipoTrabajoRequest;
import com.labgarcias.catalogos.dto.TipoTrabajoRequest;
import com.labgarcias.catalogos.dto.TipoTrabajoResponse;
import com.labgarcias.catalogos.service.TipoTrabajoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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

    @GetMapping("/todos")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Listar todos los tipos de trabajo, activos e inactivos (CU-16)",
            description = "Vista de administración: incluye los desactivados para poder reactivarlos."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Listado completo") })
    public ResponseEntity<List<TipoTrabajoResponse>> listarTodos() {
        return ResponseEntity.ok(tipoTrabajoService.listarTodos());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Crear un tipo de trabajo (CU-16)",
            description = "RN-12: mínimo 7 días estimados. RN-21: precio mínimo 250."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tipo de trabajo creado"),
            @ApiResponse(responseCode = "409", description = "TIPO_TRABAJO_DUPLICADO"),
            @ApiResponse(responseCode = "422", description = "DIAS_ESTIMADOS_INSUFICIENTES / PRECIO_INSUFICIENTE")
    })
    public ResponseEntity<TipoTrabajoResponse> crear(@Valid @RequestBody TipoTrabajoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoTrabajoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Editar un tipo de trabajo (CU-16)",
            description = "No cambia órdenes ya emitidas: precio_base y dias_estimados_aplicados quedan congelados en cada orden."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo de trabajo actualizado"),
            @ApiResponse(responseCode = "404", description = "TIPO_TRABAJO_NO_ENCONTRADO"),
            @ApiResponse(responseCode = "409", description = "TIPO_TRABAJO_DUPLICADO"),
            @ApiResponse(responseCode = "422", description = "DIAS_ESTIMADOS_INSUFICIENTES / PRECIO_INSUFICIENTE")
    })
    public ResponseEntity<TipoTrabajoResponse> actualizar(@PathVariable Integer id, @Valid @RequestBody TipoTrabajoRequest request) {
        return ResponseEntity.ok(tipoTrabajoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Activar o desactivar un tipo de trabajo (CU-16 A1)",
            description = "No elimina el registro: un tipo desactivado deja de ofrecerse en órdenes nuevas, pero las existentes conservan su referencia."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "404", description = "TIPO_TRABAJO_NO_ENCONTRADO")
    })
    public ResponseEntity<TipoTrabajoResponse> cambiarEstado(@PathVariable Integer id,
                                                              @Valid @RequestBody CambiarEstadoTipoTrabajoRequest request) {
        return ResponseEntity.ok(tipoTrabajoService.cambiarEstado(id, request.activo()));
    }
}
