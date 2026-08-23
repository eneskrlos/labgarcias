package com.labgarcias.notificaciones.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.notificaciones.dto.ContadorNotificacionesResponse;
import com.labgarcias.notificaciones.dto.NotificacionResponse;
import com.labgarcias.notificaciones.service.BandejaNotificacionService;
import com.labgarcias.shared.dto.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * §6.4: la campana. Los cuatro endpoints son de cualquier usuario autenticado y operan siempre
 * sobre lo propio: el destinatario sale del token y ninguna ruta acepta un id de usuario.
 */
@RestController
@RequestMapping("/api/v1/notificaciones")
@Tag(name = "Notificaciones")
public class NotificacionController {

    private final BandejaNotificacionService bandejaNotificacionService;

    public NotificacionController(BandejaNotificacionService bandejaNotificacionService) {
        this.bandejaNotificacionService = bandejaNotificacionService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Listar mis notificaciones (§6.4)",
            description = "Criterio 3 de §6: devuelve siempre y únicamente las notificaciones del usuario "
                    + "autenticado. El destinatario se toma del token y llega hasta la consulta, así que no "
                    + "hay manera de pedir las de otro. "
                    + "El filtro `leidas` es opcional: sin él vienen todas. "
                    + "Ordenadas de la más reciente a la más vieja. size solo admite 10, 20 o 30."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de notificaciones propias"),
            @ApiResponse(responseCode = "400", description = "TAMANO_PAGINA_INVALIDO")
    })
    public ResponseEntity<PaginaResponse<NotificacionResponse>> listarMias(
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "false trae solo las no leídas; true, solo las leídas; omitirlo, todas.")
            @RequestParam(required = false) Boolean leidas,
            Authentication authentication) {
        return ResponseEntity.ok(
                bandejaNotificacionService.listarMias(usuarioId(authentication), leidas, pageable));
    }

    @GetMapping("/contador")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Contador de notificaciones sin leer (§6.4)",
            description = "Lo que consume la campana del frontend con refetchInterval de 60 s. "
                    + "No implementar WebSocket ni SSE en esta versión (§6.4)."
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Cantidad de notificaciones propias sin leer"))
    public ResponseEntity<ContadorNotificacionesResponse> contarNoLeidas(Authentication authentication) {
        return ResponseEntity.ok(bandejaNotificacionService.contarNoLeidas(usuarioId(authentication)));
    }

    @PatchMapping("/{id}/leer")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Marcar una notificación como leída (§6.4)",
            description = "Criterio 3 de §6: una notificación de otro usuario responde 404, no 403, "
                    + "para no revelar que existe (mismo criterio que RN-01 en las órdenes). "
                    + "Marcarla dos veces no cambia la fecha de lectura original."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificación actualizada"),
            @ApiResponse(responseCode = "404", description = "NOTIFICACION_NO_ENCONTRADA")
    })
    public ResponseEntity<NotificacionResponse> marcarLeida(@PathVariable Long id,
                                                            Authentication authentication) {
        return ResponseEntity.ok(bandejaNotificacionService.marcarLeida(id, usuarioId(authentication)));
    }

    @PatchMapping("/leer-todas")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Marcar todas mis notificaciones como leídas (§6.4)",
            description = "Afecta solo a las del usuario autenticado. Devuelve el contador ya actualizado "
                    + "—siempre cero— para que la campana se refresque sin una segunda llamada."
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Contador actualizado"))
    public ResponseEntity<ContadorNotificacionesResponse> marcarTodasLeidas(Authentication authentication) {
        return ResponseEntity.ok(bandejaNotificacionService.marcarTodasLeidas(usuarioId(authentication)));
    }

    private Long usuarioId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
