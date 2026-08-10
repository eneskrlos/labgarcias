package com.labgarcias.seguridad.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.seguridad.dto.MensajeResponse;
import com.labgarcias.seguridad.dto.RegistroOdontologoRequest;
import com.labgarcias.seguridad.service.RegistroService;

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

    public AuthController(RegistroService registroService) {
        this.registroService = registroService;
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
}
