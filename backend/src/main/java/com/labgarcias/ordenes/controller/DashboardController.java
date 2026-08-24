package com.labgarcias.ordenes.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.ordenes.dto.PanelOdontologoResponse;
import com.labgarcias.ordenes.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * CU-02/§5.7: el panel de inicio del odontólogo.
 *
 * Va aparte de `AdminDashboardController` por el mismo motivo que `OrdenController` y
 * `AdminOrdenController` (§5.7): son dos paneles con reglas opuestas —uno devuelve solo lo del
 * odontólogo autenticado (RN-01) y el otro lo de todo el laboratorio—, y que cada uno tenga su
 * ruta y su autorización es lo que hace evidente cuál es cuál.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Paneles")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ODONTOLOGO')")
    @Operation(
            summary = "Panel de inicio del odontólogo (CU-02)",
            description = "Devuelve los indicadores del odontólogo autenticado y sus órdenes recientes. "
                    + "RN-01: el dueño se toma del token y el endpoint no acepta un id de odontólogo por "
                    + "parámetro, así que no hay manera de pedir el panel de otro. "
                    + "RN-22: las órdenes recientes identifican al paciente por iniciales y código. "
                    + "§8: los contadores los calcula el backend; el cliente no deriva ninguno. "
                    + "**Sin contador de mensajes nuevos**: CU-02 lo enumera pero D-11 pospuso la "
                    + "mensajería. `enCurso` excluye las órdenes en LISTO, que van en `listasParaRetirar`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Panel del odontólogo"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    })
    public ResponseEntity<PanelOdontologoResponse> panel(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.panelDelOdontologo((Long) authentication.getPrincipal()));
    }
}
