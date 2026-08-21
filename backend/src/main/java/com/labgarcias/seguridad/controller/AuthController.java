package com.labgarcias.seguridad.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.seguridad.dto.CambiarPasswordRequest;
import com.labgarcias.seguridad.dto.LoginRequest;
import com.labgarcias.seguridad.dto.LoginResponse;
import com.labgarcias.seguridad.dto.MensajeResponse;
import com.labgarcias.seguridad.dto.SolicitudAccesoRequest;
import com.labgarcias.seguridad.service.LoginService;
import com.labgarcias.seguridad.service.PasswordService;
import com.labgarcias.seguridad.service.SolicitudAccesoService;

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
    private final SolicitudAccesoService solicitudAccesoService;
    private final PasswordService passwordService;

    public AuthController(LoginService loginService,
                          SolicitudAccesoService solicitudAccesoService,
                          PasswordService passwordService) {
        this.loginService = loginService;
        this.solicitudAccesoService = solicitudAccesoService;
        this.passwordService = passwordService;
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

    @PostMapping("/solicitud-acceso")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(
            summary = "Solicitar acceso al laboratorio (D-17)",
            description = "Formulario público que reemplaza al auto-registro. Registra la solicitud y avisa "
                    + "al administrador por sus canales activos (§6.2). **No crea ningún usuario** ni habilita "
                    + "ningún inicio de sesión: las cuentas las da de alta el administrador (§3.1.b). "
                    + "Sin captcha en esta versión (§3.1)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud registrada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "CORREO_YA_REGISTRADO, SOLICITUD_YA_EXISTENTE")
    })
    public ResponseEntity<MensajeResponse> solicitarAcceso(@Valid @RequestBody SolicitudAccesoRequest request) {
        solicitudAccesoService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MensajeResponse("Solicitud enviada. El laboratorio se pondrá en contacto."));
    }

    @PostMapping("/cambiar-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cambiar la contraseña (§3.1.b)",
            description = "Cambio obligatorio del primer ingreso: mientras no se haga, el token no habilita "
                    + "ningún otro endpoint. Valida la contraseña actual, exige RN-15 en la nueva, apaga la "
                    + "bandera y devuelve un token normal, sin la restricción."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña cambiada; token nuevo sin restricción"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "422", description = "PASSWORD_ACTUAL_INCORRECTA"),
            @ApiResponse(responseCode = "400", description = "PASSWORD_INVALIDA (RN-15)")
    })
    public ResponseEntity<LoginResponse> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(passwordService.cambiar((Long) authentication.getPrincipal(), request));
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
