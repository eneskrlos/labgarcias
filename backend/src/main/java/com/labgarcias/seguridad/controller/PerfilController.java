package com.labgarcias.seguridad.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.seguridad.dto.PerfilResponse;
import com.labgarcias.seguridad.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * §7: el perfil propio. El usuario sale siempre del token: no hay ruta que acepte un id ajeno.
 *
 * Solo la lectura. `PUT /api/v1/perfil` —editar nombre y dirección— es de **T-28**, que extiende
 * esta pantalla en vez de crearla; acá está lo que §6.5 necesita para mostrar el estado de
 * Telegram.
 */
@RestController
@RequestMapping("/api/v1/perfil")
@Tag(name = "Seguridad")
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Mi perfil (§7)",
            description = "Datos propios del usuario autenticado, incluido el estado de vinculación con "
                    + "Telegram que pide §6.5. No devuelve el chat de Telegram ni el hash de la contraseña."
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Perfil del usuario autenticado"))
    public ResponseEntity<PerfilResponse> obtenerMio(Authentication authentication) {
        return ResponseEntity.ok(usuarioService.obtenerPerfil((Long) authentication.getPrincipal()));
    }
}
