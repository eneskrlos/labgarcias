package com.labgarcias.ordenes.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.ordenes.dto.CrearOrdenRequest;
import com.labgarcias.ordenes.dto.OrdenResponse;
import com.labgarcias.ordenes.service.OrdenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/ordenes")
@Tag(name = "Órdenes")
public class OrdenController {

    private final OrdenService ordenService;

    public OrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ODONTOLOGO')")
    @Operation(
            summary = "Crear una nueva orden (CU-09)",
            description = "El odontólogo crea una orden. El estado inicial, el recargo, los precios y la "
                    + "fecha estimada los calcula el backend (RN-11, RN-18, RN-21). "
                    + "RN-01: la orden se asocia siempre al odontólogo autenticado, nunca a un id recibido. "
                    + "RN-22: la respuesta no incluye el nombre del paciente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orden creada"),
            @ApiResponse(responseCode = "400", description = "VALIDACION"),
            @ApiResponse(responseCode = "422", description = "TIPO_TRABAJO_INACTIVO")
    })
    public ResponseEntity<OrdenResponse> crear(@Valid @RequestBody CrearOrdenRequest request,
                                               Authentication authentication) {
        Long odontologoId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenService.crear(request, odontologoId));
    }
}
