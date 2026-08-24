package com.labgarcias.ordenes.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.ordenes.dto.DashboardAdminResponse;
import com.labgarcias.ordenes.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** §5.7/CU-10: el dashboard del laboratorio. Ver `DashboardController` por qué son dos controllers. */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@Tag(name = "Paneles")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Dashboard del laboratorio (CU-10, §5.7)",
            description = "Devuelve las cuatro cosas de §5.7 sobre las órdenes de todos los odontólogos: "
                    + "contadores (en curso, listas para retirar, entregadas esta semana y urgentes activas), "
                    + "distribución por estado desde la vista `v_ordenes_por_estado`, próximas a entregar "
                    + "ordenadas por fecha estimada, y órdenes recientes; más el bloque de urgentes de la "
                    + "vista `v_ordenes_urgentes`. "
                    + "Cada bloque trae hasta 5 filas: es un resumen, no un listado paginado (§8.1). "
                    + "RN-22: **ningún bloque incluye el nombre del paciente**, ni siquiera el de urgentes, "
                    + "cuya vista sí lo tiene. El laboratorio lo ve en el detalle de la orden (§5.4). "
                    + "`entregadasEstaSemana` cuenta el pasaje a ENTREGADO registrado en el historial, "
                    + "con la semana de lunes a domingo en la zona horaria del laboratorio. "
                    + "**Sin reportes ni estadísticas** más allá de estos contadores: CU-13 es Fase 4."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard del laboratorio"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    })
    public ResponseEntity<DashboardAdminResponse> dashboard() {
        return ResponseEntity.ok(dashboardService.dashboardDelLaboratorio());
    }
}
