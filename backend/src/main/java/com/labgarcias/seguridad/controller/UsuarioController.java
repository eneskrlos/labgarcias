package com.labgarcias.seguridad.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labgarcias.seguridad.dto.CambiarEstadoUsuarioRequest;
import com.labgarcias.seguridad.dto.UsuarioResponse;
import com.labgarcias.seguridad.service.UsuarioService;
import com.labgarcias.shared.dto.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * CU-17/§7: mantenimiento del padrón de cuentas, **solo SUPERADMIN**.
 *
 * Va aparte de `OdontologoController` porque son dos padrones con alcances distintos: aquel es de
 * un rol y lo opera la administración; este abarca **cualquier rol, incluidos los administradores**,
 * y §3.5 lo reserva al SuperAdmin. Que cada uno tenga su ruta y su autorización es lo que hace
 * evidente cuál es cuál — el mismo criterio de §5.7 entre las dos vistas de órdenes.
 *
 * CU-17 habla de "privilegios totales", pero **§7 fija el alcance en estos dos endpoints**: el
 * padrón y el alta/baja de una cuenta. No se inventa nada más (`Agente.md` §3.1).
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Seguridad")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(
            summary = "Listar todas las cuentas, paginado (CU-17)",
            description = "El padrón completo, de cualquier rol, ordenado por nombre. A diferencia de "
                    + "`GET /odontologos`, no filtra por rol: §7 lo reserva al mantenimiento del sistema. "
                    + "size solo admite 10, 20 o 30. **No devuelve contraseñas ni el chat de Telegram.**"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página del padrón de cuentas"),
            @ApiResponse(responseCode = "400", description = "TAMANO_PAGINA_INVALIDO"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    })
    public ResponseEntity<PaginaResponse<UsuarioResponse>> listar(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listarTodos(pageable));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(
            summary = "Activar o desactivar una cuenta (CU-17)",
            description = "Da de alta o de baja una cuenta de cualquier rol. Una cuenta INACTIVA no puede "
                    + "iniciar sesión: el login responde `403 CUENTA_INACTIVA` (§3.3). "
                    + "**El SuperAdmin no puede cambiar el estado de su propia cuenta**: es quien reactiva "
                    + "a los demás, y dejar el sistema sin ningún SUPERADMIN activo lo volvería "
                    + "irrecuperable desde la aplicación. "
                    + "`PENDIENTE_VERIFICACION` no se acepta: D-18 eliminó la verificación por correo, así "
                    + "que una cuenta en ese estado quedaría sin forma de destrabarse."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta actualizada"),
            @ApiResponse(responseCode = "400", description = "ESTADO_CUENTA_INVALIDO / VALIDACION"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "USUARIO_NO_ENCONTRADO"),
            @ApiResponse(responseCode = "422", description = "AUTODESACTIVACION_NO_PERMITIDA")
    })
    public ResponseEntity<UsuarioResponse> cambiarEstado(@PathVariable Long id,
                                                          @Valid @RequestBody CambiarEstadoUsuarioRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(
                id, request.estadoCuenta(), (Long) authentication.getPrincipal()));
    }
}
