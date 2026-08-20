package com.labgarcias.notificaciones.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.notificaciones.dto.ConfiguracionNotificacionRequest;
import com.labgarcias.notificaciones.dto.ConfiguracionNotificacionResponse;
import com.labgarcias.notificaciones.service.ConfiguracionNotificacionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * RN-19/CU-21: §6.4 reserva estos dos endpoints a ADMIN y SUPERADMIN. La ruta no lleva id de
 * usuario: cada administrador configura la suya, tomada del token.
 */
@RestController
@RequestMapping("/api/v1/configuracion-notificaciones")
@Tag(name = "Notificaciones")
public class ConfiguracionNotificacionController {

    private final ConfiguracionNotificacionService configuracionNotificacionService;

    public ConfiguracionNotificacionController(ConfiguracionNotificacionService configuracionNotificacionService) {
        this.configuracionNotificacionService = configuracionNotificacionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Ver mi configuración de canales (RN-19)",
            description = "Quien nunca guardó una configuración recibe los canales por defecto de §6.3 "
                    + "(app + correo + Telegram) con fechaActualizacion nula, no un 404: son los canales "
                    + "por los que efectivamente está recibiendo. "
                    + "P-18: canalWhatsappActivo se informa pero no se puede activar."
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Configuración vigente"))
    public ResponseEntity<ConfiguracionNotificacionResponse> obtenerMia(Authentication authentication) {
        return ResponseEntity.ok(configuracionNotificacionService.obtenerMia(usuarioId(authentication)));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Guardar mi configuración de canales (CU-21)",
            description = "Reemplaza la configuración entera: las tres banderas son obligatorias y el chat "
                    + "de Telegram que no venga no queda guardado. Crea la configuración la primera vez. "
                    + "Criterio 4 de §6: activar Telegram sin telegramChatId devuelve 422 TELEGRAM_SIN_DESTINO. "
                    + "P-18: WhatsApp no se puede activar mientras sea solo estructura."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuración guardada"),
            @ApiResponse(responseCode = "400", description = "VALIDACION"),
            @ApiResponse(responseCode = "422", description = "TELEGRAM_SIN_DESTINO")
    })
    public ResponseEntity<ConfiguracionNotificacionResponse> guardarMia(
            @Valid @RequestBody ConfiguracionNotificacionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(configuracionNotificacionService.guardarMia(usuarioId(authentication), request));
    }

    private Long usuarioId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
