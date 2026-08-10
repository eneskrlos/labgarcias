package com.labgarcias.seguridad.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.seguridad.dto.MensajeResponse;
import com.labgarcias.seguridad.dto.ReenviarVerificacionRequest;
import com.labgarcias.seguridad.dto.RegistroOdontologoRequest;
import com.labgarcias.seguridad.service.RegistroService;
import com.labgarcias.seguridad.service.VerificacionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Seguridad")
public class AuthController {

    private final RegistroService registroService;
    private final VerificacionService verificacionService;

    public AuthController(RegistroService registroService, VerificacionService verificacionService) {
        this.registroService = registroService;
        this.verificacionService = verificacionService;
    }

    @PostMapping("/registro")
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
}
