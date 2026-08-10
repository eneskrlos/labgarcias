package com.labgarcias.licencia.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.licencia.dto.LicenciaResponse;
import com.labgarcias.licencia.dto.LicenciaVigenteResponse;
import com.labgarcias.licencia.dto.RegistrarLicenciaRequest;
import com.labgarcias.licencia.service.LicenciaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/licencias")
@Tag(name = "Licencia")
public class LicenciaController {

    private final LicenciaService licenciaService;

    public LicenciaController(LicenciaService licenciaService) {
        this.licenciaService = licenciaService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(
            summary = "Listado histórico de licencias (CU-23)",
            description = "Todos los períodos registrados, más reciente primero."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Listado histórico") })
    public ResponseEntity<List<LicenciaResponse>> listar() {
        return ResponseEntity.ok(licenciaService.listarHistorico());
    }

    @GetMapping("/vigente")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(
            summary = "Estado actual de la licencia (CU-23)",
            description = "Indica si hay un período vigente hoy y, si lo hay, sus datos."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Estado actual") })
    public ResponseEntity<LicenciaVigenteResponse> vigente() {
        return ResponseEntity.ok(licenciaService.obtenerVigente());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(
            summary = "Registrar un período de licencia (CU-23)",
            description = "RN-20: activa o renueva la licencia de esta instalación (D-16: una instalación, sin planes ni pasarela de pago)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Licencia registrada"),
            @ApiResponse(responseCode = "422", description = "FECHAS_LICENCIA_INVALIDAS")
    })
    public ResponseEntity<LicenciaResponse> registrar(@Valid @RequestBody RegistrarLicenciaRequest request,
                                                        Authentication authentication) {
        Long usuarioId = (Long) authentication.getPrincipal();
        LicenciaResponse creada = licenciaService.registrar(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }
}
