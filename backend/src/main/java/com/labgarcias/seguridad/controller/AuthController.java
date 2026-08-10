package com.labgarcias.seguridad.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.seguridad.dto.GoogleAuthRequest;
import com.labgarcias.seguridad.dto.LoginRequest;
import com.labgarcias.seguridad.dto.LoginResponse;
import com.labgarcias.seguridad.dto.MensajeResponse;
import com.labgarcias.seguridad.dto.ReenviarVerificacionRequest;
import com.labgarcias.seguridad.dto.RegistroOdontologoRequest;
import com.labgarcias.seguridad.service.GoogleAuthService;
import com.labgarcias.seguridad.service.LoginService;
import com.labgarcias.seguridad.service.RegistroService;
import com.labgarcias.seguridad.service.VerificacionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Seguridad")
public class AuthController {

    private final RegistroService registroService;
    private final VerificacionService verificacionService;
    private final LoginService loginService;
    private final GoogleAuthService googleAuthService;

    public AuthController(RegistroService registroService, VerificacionService verificacionService,
                           LoginService loginService, GoogleAuthService googleAuthService) {
        this.registroService = registroService;
        this.verificacionService = verificacionService;
        this.loginService = loginService;
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/registro")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(
            summary = "Registrar un odontólogo (CU-18)",
            description = "Auto-registro del odontólogo. La cuenta queda PENDIENTE_VERIFICACION hasta "
                    + "confirmar el enlace enviado por correo (CU-19)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada"),
            @ApiResponse(responseCode = "400", description = "PASSWORD_INVALIDA"),
            @ApiResponse(responseCode = "409", description = "CORREO_YA_REGISTRADO / USUARIO_YA_REGISTRADO")
    })
    public ResponseEntity<MensajeResponse> registrar(@Valid @RequestBody RegistroOdontologoRequest request) {
        registroService.registrarOdontologo(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MensajeResponse("Cuenta creada. Revisá tu correo para confirmarla."));
    }

    @GetMapping("/verificar")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(
            summary = "Verificar la cuenta con el enlace recibido por correo (CU-19)",
            description = "Activa la cuenta si el token existe, no fue usado y no está vencido (D-02: 24 h de vigencia)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta verificada"),
            @ApiResponse(responseCode = "400", description = "TOKEN_INVALIDO")
    })
    public ResponseEntity<MensajeResponse> verificar(@RequestParam String token) {
        verificacionService.verificar(token);
        return ResponseEntity.ok(new MensajeResponse("Cuenta verificada correctamente. Ya podés iniciar sesión."));
    }

    @PostMapping("/reenviar-verificacion")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(
            summary = "Reenviar el enlace de verificación (CU-19 A1)",
            description = "Invalida los tokens pendientes del usuario y emite uno nuevo. Responde igual exista "
                    + "o no la cuenta, para no revelar su existencia."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud procesada")
    })
    public ResponseEntity<MensajeResponse> reenviarVerificacion(@Valid @RequestBody ReenviarVerificacionRequest request) {
        verificacionService.reenviarVerificacion(request.correo());
        return ResponseEntity.ok(new MensajeResponse("Si el correo está registrado, vas a recibir un nuevo enlace de verificación."));
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
            @ApiResponse(responseCode = "403", description = "CUENTA_NO_VERIFICADA")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.login(request));
    }

    @PostMapping("/google")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(
            summary = "Iniciar sesión o registrarse con Google (RN-16)",
            description = "Valida el idToken con Google. Si el google_subject_id ya existe, es un login; si no, "
                    + "crea la cuenta como ACTIVA y con el correo verificado (CU-19 A2: Google ya lo verificó)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login o registro exitoso"),
            @ApiResponse(responseCode = "401", description = "GOOGLE_TOKEN_INVALIDO"),
            @ApiResponse(responseCode = "409", description = "CORREO_YA_REGISTRADO")
    })
    public ResponseEntity<LoginResponse> google(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(googleAuthService.autenticar(request.idToken()));
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
