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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.ordenes.dto.CambiarEstadoOrdenRequest;
import com.labgarcias.ordenes.dto.CrearOrdenRequest;
import com.labgarcias.ordenes.dto.OrdenDetalleResponse;
import com.labgarcias.ordenes.dto.OrdenListadoResponse;
import com.labgarcias.ordenes.dto.OrdenResponse;
import com.labgarcias.ordenes.service.OrdenEstadoService;
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
    private final OrdenEstadoService ordenEstadoService;

    public OrdenController(OrdenService ordenService, OrdenEstadoService ordenEstadoService) {
        this.ordenService = ordenService;
        this.ordenEstadoService = ordenEstadoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Registrar una nueva orden (CU-09, D-19)",
            description = "D-19: la orden la registra el laboratorio, que indica a qué odontólogo pertenece "
                    + "mediante odontologoId; el odontólogo la envía en papel o por teléfono. "
                    + "El id debe ser de una cuenta ODONTOLOGO activa, si no responde 422 ODONTOLOGO_INVALIDO. "
                    + "El estado inicial, el recargo, los precios y la fecha estimada los calcula el backend "
                    + "(RN-11, RN-18, RN-21). RN-22: la respuesta no incluye el nombre del paciente. "
                    + "P-19: si se reabre la creación por el odontólogo, vuelve el flujo original de CU-09."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orden creada"),
            @ApiResponse(responseCode = "400", description = "VALIDACION"),
            @ApiResponse(responseCode = "422", description = "ODONTOLOGO_INVALIDO / TIPO_TRABAJO_INACTIVO")
    })
    public ResponseEntity<OrdenResponse> crear(@Valid @RequestBody CrearOrdenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenService.crear(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ODONTOLOGO')")
    @Operation(
            summary = "Listar mis órdenes (CU-03) e historial (CU-12)",
            description = "RN-01: devuelve siempre y únicamente las órdenes del odontólogo autenticado. "
                    + "El id del dueño se toma del token; el endpoint no acepta un id de odontólogo por "
                    + "parámetro, así que no hay manera de pedir las de otro. "
                    + "RN-22: cada ítem identifica al paciente por iniciales y código, nunca por su nombre. "
                    + "CU-12: con `historico=true` quedan solo las órdenes cerradas, que son las de estado "
                    + "terminal según `estado.es_terminal` (hoy ENTREGADO y CANCELADO). Los dos filtros se "
                    + "combinan: `historico=true&estado=CANCELADO` deja las canceladas. "
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
            @Parameter(description = "CU-12: true deja solo las órdenes ya cerradas (estado terminal). "
                    + "Omitirlo trae todas, que es el listado de CU-03.")
            @RequestParam(required = false, defaultValue = "false") boolean historico,
            Authentication authentication) {
        return ResponseEntity.ok(
                ordenService.listarMisOrdenes(usuarioId(authentication), estado, historico, pageable));
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

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Avanzar el estado de una orden (CU-06)",
            description = "RN-04: flujo lineal. Solo se admite el estado con la secuencia inmediatamente "
                    + "superior a la actual; saltear etapas y retroceder devuelven 409 (P-02 sin resolver). "
                    + "Desde un estado terminal (ENTREGADO, CANCELADO) no hay transición. "
                    + "RN-17/CU-20: el laboratorio no puede cancelar; pedir CANCELADO por acá da 409. "
                    + "En la misma transacción se actualiza el estado, se registra el historial con el "
                    + "autor y se publica el evento de cambio de estado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden actualizada, con su línea de tiempo"),
            @ApiResponse(responseCode = "404", description = "ORDEN_NO_ENCONTRADA"),
            @ApiResponse(responseCode = "409", description = "TRANSICION_NO_PERMITIDA"),
            @ApiResponse(responseCode = "422", description = "ESTADO_NO_ENCONTRADO")
    })
    public ResponseEntity<OrdenDetalleResponse> avanzarEstado(@PathVariable Long id,
                                                              @Valid @RequestBody CambiarEstadoOrdenRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(ordenEstadoService.avanzarEstado(
                id, request.estadoCodigo(), usuarioId(authentication)));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ODONTOLOGO')")
    @Operation(
            summary = "Cancelar una orden (CU-20)",
            description = "RN-17: la orden es inmutable; cancelarla es lo único que el odontólogo puede "
                    + "hacer sobre una orden ya creada, y no existe endpoint de edición. "
                    + "RN-01: una orden ajena responde 404, no 403. "
                    + "Una orden en estado terminal (ENTREGADO o CANCELADO) devuelve 409. "
                    + "P-14 sin resolver: no se calcula ningún cargo por cancelación."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden cancelada, con su línea de tiempo"),
            @ApiResponse(responseCode = "404", description = "ORDEN_NO_ENCONTRADA"),
            @ApiResponse(responseCode = "409", description = "ORDEN_NO_CANCELABLE")
    })
    public ResponseEntity<OrdenDetalleResponse> cancelar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ordenEstadoService.cancelar(id, usuarioId(authentication)));
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
