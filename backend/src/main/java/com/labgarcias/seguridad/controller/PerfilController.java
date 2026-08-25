package com.labgarcias.seguridad.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.seguridad.dto.ActualizarPerfilRequest;
import com.labgarcias.seguridad.dto.PerfilResponse;
import com.labgarcias.seguridad.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * §7: el perfil propio. El usuario sale siempre del token: no hay ruta que acepte un id ajeno.
 *
 * La lectura la entregó T-32b con el estado de Telegram de §6.5; **T-28 sumó el `PUT`**.
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

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Editar mi perfil (§7)",
            description = "Actualiza **solo `nombreCompleto` y `direccion`** del usuario autenticado. "
                    + "§7: no se puede cambiar el rol ni el correo, y no están en el request — mandarlos "
                    + "en el cuerpo no tiene efecto. Tampoco el nombre de usuario, el teléfono ni el "
                    + "estado de la cuenta. El id sale del token: no hay forma de editar el perfil de otro."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado"),
            @ApiResponse(responseCode = "400", description = "VALIDACION")
    })
    public ResponseEntity<PerfilResponse> actualizarMio(@Valid @RequestBody ActualizarPerfilRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(
                usuarioService.actualizarPerfil((Long) authentication.getPrincipal(), request));
    }
}
