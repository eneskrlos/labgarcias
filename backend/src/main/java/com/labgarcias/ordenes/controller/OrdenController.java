package com.labgarcias.ordenes.controller;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.ordenes.dto.CrearOrdenRequest;
import com.labgarcias.ordenes.dto.OrdenDetalleResponse;
import com.labgarcias.ordenes.dto.OrdenListadoResponse;
import com.labgarcias.ordenes.dto.OrdenResponse;
import com.labgarcias.ordenes.service.OrdenService;
import com.labgarcias.shared.dto.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/ordenes")
@Tag(name = "Órdenes")
public class OrdenController {

    private static final Set<String> ROLES_ADMINISTRACION = Set.of("ROLE_ADMIN", "ROLE_SUPERADMIN");

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

    @GetMapping
    @PreAuthorize("hasRole('ODONTOLOGO')")
    @Operation(
            summary = "Listar mis órdenes (CU-03)",
            description = "RN-01: devuelve siempre y únicamente las órdenes del odontólogo autenticado. "
                    + "El id del dueño se toma del token; el endpoint no acepta un id de odontólogo por "
                    + "parámetro, así que no hay manera de pedir las de otro. "
                    + "RN-22: cada ítem identifica al paciente por iniciales y código, nunca por su nombre. "
                    + "size solo admite 10, 20 o 30. Ordenado por fecha de ingreso descendente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de órdenes propias"),
            @ApiResponse(responseCode = "400", description = "TAMANO_PAGINA_INVALIDO")
    })
    public ResponseEntity<PaginaResponse<OrdenListadoResponse>> listarMisOrdenes(
            @PageableDefault(size = 10, sort = "fechaIngreso", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "Código del estado (RECIBIDO, EN_EVALUACION, EN_PRODUCCION, "
                    + "CONTROL_CALIDAD, LISTO, ENTREGADO, CANCELADO). Un código inexistente devuelve página vacía.")
            @RequestParam(required = false) String estado,
            Authentication authentication) {
        return ResponseEntity.ok(ordenService.listarMisOrdenes(usuarioId(authentication), estado, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','ODONTOLOGO')")
    @Operation(
            summary = "Detalle y seguimiento de una orden (CU-04)",
            description = "Suma a los datos del listado la descripción, el desglose de precios, los adjuntos "
                    + "y la línea de tiempo fechada con el autor de cada etapa. "
                    + "RN-01: un odontólogo que pide una orden ajena recibe 404, no 403, para no revelar que existe. "
                    + "RN-22: pacienteNombre solo aparece para ADMIN y SUPERADMIN; en la respuesta al odontólogo "
                    + "la clave no viene."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la orden"),
            @ApiResponse(responseCode = "404", description = "ORDEN_NO_ENCONTRADA")
    })
    public ResponseEntity<OrdenDetalleResponse> obtenerDetalle(@PathVariable Long id,
                                                               Authentication authentication) {
        return ResponseEntity.ok(ordenService.obtenerDetalle(
                id, usuarioId(authentication), esAdministrador(authentication)));
    }

    private Long usuarioId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    private boolean esAdministrador(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLES_ADMINISTRACION::contains);
    }
}
