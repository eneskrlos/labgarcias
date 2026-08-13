package com.labgarcias.catalogos.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.catalogos.dto.CambiarEstadoTipoTrabajoRequest;
import com.labgarcias.catalogos.dto.TipoTrabajoRequest;
import com.labgarcias.catalogos.dto.TipoTrabajoResponse;
import com.labgarcias.catalogos.service.TipoTrabajoService;
import com.labgarcias.shared.dto.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @GetMapping("/activos")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Listar tipos de trabajo activos (CU-16, CU-09)",
            description = "Catálogo completo sin paginar: alimenta el selector de tipo de trabajo al crear una orden "
                    + "(el odontólogo necesita verlo entero). Los tipos desactivados no aparecen. "
                    + "Excepción deliberada a la paginación general (spec.md §4.1)."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Listado completo de tipos activos") })
    public ResponseEntity<List<TipoTrabajoResponse>> listarActivos() {
        return ResponseEntity.ok(tipoTrabajoService.listarActivos());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Listar tipos de trabajo, paginado (CU-16)",
            description = "Vista de administración: incluye activos e inactivos. size solo admite 10, 20 o 30."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de tipos de trabajo"),
            @ApiResponse(responseCode = "400", description = "TAMANO_PAGINA_INVALIDO")
    })
    public ResponseEntity<PaginaResponse<TipoTrabajoResponse>> listarPaginado(
            @PageableDefault(size = 10, sort = "nombre") Pageable pageable,
            @Parameter(description = "Filtra por activo/inactivo") @RequestParam(required = false) Boolean activo,
            @Parameter(description = "Coincidencia parcial del nombre, sin distinguir mayúsculas ni acentos")
            @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(tipoTrabajoService.listarPaginado(pageable, activo, busqueda));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Obtener un tipo de trabajo por id (CU-16)",
            description = "Alimenta la precarga del formulario de edición (spec.md §8.1: rutas de alta/edición separadas del listado)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo de trabajo encontrado"),
            @ApiResponse(responseCode = "404", description = "TIPO_TRABAJO_NO_ENCONTRADO")
    })
    public ResponseEntity<TipoTrabajoResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(tipoTrabajoService.obtenerPorId(id));
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
