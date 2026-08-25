package com.labgarcias.seguridad.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.seguridad.dto.CrearOdontologoRequest;
import com.labgarcias.seguridad.dto.OdontologoActivoResponse;
import com.labgarcias.seguridad.dto.OdontologoResponse;
import com.labgarcias.seguridad.service.OdontologoService;
import com.labgarcias.shared.dto.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * D-18/§3.1.b y CU-11/§7: alta de cuentas de odontólogo y los **dos listados**, que conviven
 * porque tienen propósitos distintos:
 *
 * - `GET /odontologos` — la tabla administrable de CU-11: paginada, con los datos de cada cuenta y
 *   también las dadas de baja, que es donde se las vuelve a activar.
 * - `GET /odontologos/activos` — el selector de §5.1 (D-19): sin paginar, solo cuentas ACTIVA, y
 *   únicamente `id` y `nombreCompleto`, que es lo que un selector necesita.
 */
@RestController
@RequestMapping("/api/v1/odontologos")
@Tag(name = "Seguridad")
public class OdontologoController {

    private final OdontologoService odontologoService;

    public OdontologoController(OdontologoService odontologoService) {
        this.odontologoService = odontologoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Listar odontólogos, paginado (CU-11)",
            description = "La tabla administrable de §7: los datos de cada cuenta, ordenadas por nombre. "
                    + "**Incluye las cuentas dadas de baja**, que es donde se las vuelve a activar. "
                    + "Es otra cosa que `GET /odontologos/activos`, que alimenta el selector de §5.1 "
                    + "sin paginar y solo con las ACTIVA. size solo admite 10, 20 o 30. "
                    + "No devuelve la contraseña ni su hash."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de odontólogos"),
            @ApiResponse(responseCode = "400", description = "TAMANO_PAGINA_INVALIDO"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    })
    public ResponseEntity<PaginaResponse<OdontologoResponse>> listar(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(odontologoService.listar(pageable));
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Listar odontólogos activos para el selector de nueva orden (§5.1, D-19)",
            description = "Listado completo **sin paginar**: alimenta el selector de odontólogo al registrar una "
                    + "orden, donde el laboratorio necesita verlos a todos. Excepción a la paginación general, "
                    + "por el mismo motivo que `/tipos-trabajo/activos` (§4). "
                    + "Devuelve solo id y nombre: un selector no necesita datos de contacto. "
                    + "Las cuentas dadas de baja no aparecen, porque `POST /ordenes` las rechaza."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado completo de odontólogos activos"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    })
    public ResponseEntity<List<OdontologoActivoResponse>> listarActivos() {
        return ResponseEntity.ok(odontologoService.listarActivos());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Crear la cuenta de un odontólogo (D-18)",
            description = "Crea la cuenta con rol ODONTOLOGO, estado ACTIVA y contraseña **generada por el "
                    + "sistema** (RN-15, `SecureRandom`), que se envía por correo al odontólogo y obliga a "
                    + "cambiarla en el primer ingreso. La respuesta **no incluye la contraseña**. "
                    + "Si se envía `solicitudId`, esa solicitud de acceso pasa a APROBADA (criterio 4). "
                    + "No existe endpoint para regenerar credenciales: si el correo falla, se vuelve a crear "
                    + "la cuenta (§3.1.b)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada; credenciales enviadas por correo"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "SOLICITUD_NO_ENCONTRADA"),
            @ApiResponse(responseCode = "409", description = "CORREO_YA_REGISTRADO, NOMBRE_USUARIO_YA_REGISTRADO, "
                    + "SOLICITUD_YA_RESUELTA")
    })
    public ResponseEntity<OdontologoResponse> crear(@Valid @RequestBody CrearOdontologoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(odontologoService.crear(request));
    }
}
