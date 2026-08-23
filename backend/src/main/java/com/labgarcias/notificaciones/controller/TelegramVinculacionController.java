package com.labgarcias.notificaciones.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.notificaciones.dto.VinculacionTelegramResponse;
import com.labgarcias.notificaciones.service.VinculacionTelegramService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * §6.5: los dos endpoints de la vinculación. Los dos operan siempre sobre el usuario del token y
 * ninguna ruta acepta un id: vincular el Telegram de otro sería recibir sus notificaciones.
 */
@RestController
@RequestMapping("/api/v1/telegram/vinculacion")
@Tag(name = "Notificaciones")
public class TelegramVinculacionController {

    private final VinculacionTelegramService vinculacionTelegramService;

    public TelegramVinculacionController(VinculacionTelegramService vinculacionTelegramService) {
        this.vinculacionTelegramService = vinculacionTelegramService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Pedir el enlace para conectar Telegram (§6.5)",
            description = "Emite un token de un solo uso, con 15 minutos de vigencia, y devuelve el enlace "
                    + "profundo `https://t.me/{bot}?start={token}`. El usuario lo abre y toca Iniciar; el "
                    + "backend recibe ese `/start` por `getUpdates` y guarda su chat. "
                    + "Un bot no puede escribir primero: por eso hace falta este paso."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enlace profundo al bot"),
            @ApiResponse(responseCode = "422", description = "TELEGRAM_NO_CONFIGURADO: falta el bot (P-20)")
    })
    public ResponseEntity<VinculacionTelegramResponse> generarEnlace(Authentication authentication) {
        return ResponseEntity.ok(vinculacionTelegramService.generarEnlace(usuarioId(authentication)));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Desvincular Telegram (§6.5)",
            description = "Limpia el chat y la bandera del usuario autenticado. Criterio 3 de §6.5: los "
                    + "envíos por Telegram pasan a quedar FALLIDO con \"Telegram no vinculado\", sin afectar "
                    + "el correo ni la campana. Desvincular dos veces no es un error."
    )
    @ApiResponses(@ApiResponse(responseCode = "204", description = "Telegram desvinculado"))
    public ResponseEntity<Void> desvincular(Authentication authentication) {
        vinculacionTelegramService.desvincular(usuarioId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long usuarioId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
