package com.labgarcias.ordenes.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.ordenes.dto.OrdenListadoResponse;
import com.labgarcias.ordenes.service.OrdenService;
import com.labgarcias.shared.dto.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * §5.7/CU-06: las órdenes vistas por el laboratorio.
 *
 * Va aparte de `OrdenController` porque son dos vistas distintas del mismo recurso: aquella
 * devuelve **solo las del odontólogo autenticado** (RN-01) y esta las de todos, filtrables. Que
 * cada una tenga su ruta y su autorización es lo que hace evidente cuál es cuál.
 *
 * El detalle y el avance de estado no están acá: `GET /ordenes/{id}` ya admite al administrador
 * —y le agrega el nombre del paciente (§5.4)— y `PATCH /ordenes/{id}/estado` es de §5.5.
 */
@RestController
@RequestMapping("/api/v1/admin/ordenes")
@Tag(name = "Órdenes")
public class AdminOrdenController {

    private final OrdenService ordenService;

    public AdminOrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Listar las órdenes del laboratorio, paginado (CU-06, §5.7)",
            description = "Los tres filtros son opcionales y se combinan. A diferencia de `GET /ordenes`, "
                    + "acá `odontologoId` sí es un parámetro: quien consulta es la administración, que ve "
                    + "todas las órdenes por rol. "
                    + "**No devuelve el nombre del paciente**: ningún listado lo incluye (RN-22); el "
                    + "laboratorio lo ve en el detalle. size solo admite 10, 20 o 30. "
                    + "Ordenado por fecha de ingreso descendente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de órdenes del laboratorio"),
            @ApiResponse(responseCode = "400", description = "TAMANO_PAGINA_INVALIDO, TIPO_ORDEN_INVALIDO"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    })
    public ResponseEntity<PaginaResponse<OrdenListadoResponse>> listar(
            @PageableDefault(size = 10, sort = "fechaIngreso", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "Código del estado (RECIBIDO, EN_EVALUACION, ...). Omitirlo trae todos.")
            @RequestParam(required = false) String estado,
            @Parameter(description = "NORMAL o URGENTE. Omitirlo trae los dos.")
            @RequestParam(required = false) String tipoOrden,
            @Parameter(description = "Id del odontólogo dueño. Omitirlo trae los de todos.")
            @RequestParam(required = false) Long odontologoId) {
        return ResponseEntity.ok(
                ordenService.listarParaAdministracion(estado, tipoOrden, odontologoId, pageable));
    }
}
