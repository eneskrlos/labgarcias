package com.labgarcias.seguridad.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.seguridad.dto.SolicitudAccesoResponse;
import com.labgarcias.seguridad.service.SolicitudAccesoService;
import com.labgarcias.shared.dto.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * §3.1.b "Gestión de solicitudes": lo que el administrador hace con los pedidos de acceso.
 *
 * Acá no está la aprobación: aprobar una solicitud es crear la cuenta, y eso es
 * `POST /api/v1/odontologos` con `solicitudId` (§3.1.b).
 */
@RestController
@RequestMapping("/api/v1/solicitudes-acceso")
@Tag(name = "Seguridad")
public class SolicitudAccesoController {

    private final SolicitudAccesoService solicitudAccesoService;

    public SolicitudAccesoController(SolicitudAccesoService solicitudAccesoService) {
        this.solicitudAccesoService = solicitudAccesoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Listar solicitudes de acceso, paginado (D-17)",
            description = "Vista de administración de §3.1.b. El filtro `estado` es opcional: sin él vienen "
                    + "todas. Ordenadas de la más reciente a la más vieja. size solo admite 10, 20 o 30."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de solicitudes"),
            @ApiResponse(responseCode = "400", description = "TAMANO_PAGINA_INVALIDO, ESTADO_SOLICITUD_INVALIDO"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    })
    public ResponseEntity<PaginaResponse<SolicitudAccesoResponse>> listarPaginado(
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "PENDIENTE, APROBADA o RECHAZADA. Omitirlo trae todas.")
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(solicitudAccesoService.listarPaginado(pageable, estado));
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Rechazar una solicitud de acceso (D-17)",
            description = "Marca la solicitud como RECHAZADA y sella su fecha de resolución. No crea ni "
                    + "modifica ninguna cuenta, y no notifica al solicitante: §6.2 no define ese aviso. "
                    + "El correo rechazado puede volver a solicitar acceso (§3.1 solo impide duplicar una pendiente)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud rechazada"),
            @ApiResponse(responseCode = "404", description = "SOLICITUD_NO_ENCONTRADA"),
            @ApiResponse(responseCode = "409", description = "SOLICITUD_YA_RESUELTA")
    })
    public ResponseEntity<SolicitudAccesoResponse> rechazar(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudAccesoService.rechazar(id));
    }
}
