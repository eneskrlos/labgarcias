package com.labgarcias.seguridad.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.seguridad.dto.CrearOdontologoRequest;
import com.labgarcias.seguridad.dto.OdontologoResponse;
import com.labgarcias.seguridad.service.OdontologoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * D-18/§3.1.b: alta de cuentas de odontólogo.
 *
 * El listado de CU-11 (`GET /api/v1/odontologos`, §7) es de otra tarea y todavía no existe.
 */
@RestController
@RequestMapping("/api/v1/odontologos")
@Tag(name = "Seguridad")
public class OdontologoController {

    private final OdontologoService odontologoService;

    public OdontologoController(OdontologoService odontologoService) {
        this.odontologoService = odontologoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Crear la cuenta de un odontólogo (D-18)",
            description = "Crea la cuenta con rol ODONTOLOGO, estado ACTIVA y contraseña **generada por el "
                    + "sistema** (RN-15, `SecureRandom`), que se envía por correo al odontólogo y obliga a "
                    + "cambiarla en el primer ingreso. La respuesta **no incluye la contraseña**. "
                    + "Si se envía `solicitudId`, esa solicitud de acceso pasa a APROBADA (criterio 4). "
                    + "No existe endpoint para regenerar credenciales: si el correo falla, se vuelve a crear "
                    + "la cuenta (§3.1.b)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada; credenciales enviadas por correo"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "SOLICITUD_NO_ENCONTRADA"),
            @ApiResponse(responseCode = "409", description = "CORREO_YA_REGISTRADO, NOMBRE_USUARIO_YA_REGISTRADO, "
                    + "SOLICITUD_YA_RESUELTA")
    })
    public ResponseEntity<OdontologoResponse> crear(@Valid @RequestBody CrearOdontologoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(odontologoService.crear(request));
    }
}
