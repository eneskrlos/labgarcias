package com.labgarcias.seguridad.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.seguridad.dto.LoginRequest;
import com.labgarcias.seguridad.dto.LoginResponse;
import com.labgarcias.seguridad.dto.MensajeResponse;
import com.labgarcias.seguridad.service.LoginService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * CR-01: el auto-registro (CU-18), la verificación por correo (CU-19) y la
 * autenticación con Google se retiraron. El alta de cuentas la hace el
 * administrador (D-18, spec.md §3.1.b) y el acceso se solicita por el
 * formulario público de D-17 (spec.md §3.1).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Seguridad")
public class AuthController {

    private final LoginService loginService;

    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(
            summary = "Iniciar sesión (CU-01)",
            description = "Valida las credenciales locales y emite un JWT con el id de usuario (sub) y el rol."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "401", description = "CREDENCIALES_INVALIDAS"),
            @ApiResponse(responseCode = "403", description = "CUENTA_INACTIVA")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.login(request));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cerrar sesión (CU-14)",
            description = "El JWT es stateless y no hay tabla de revocación en el modelo: el cierre de sesión "
                    + "es responsabilidad del cliente (descartar el token). Este endpoint solo confirma la operación."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión cerrada"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<MensajeResponse> logout() {
        return ResponseEntity.ok(new MensajeResponse("Sesión cerrada."));
    }
}
