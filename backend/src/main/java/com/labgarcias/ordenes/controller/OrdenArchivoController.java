package com.labgarcias.ordenes.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.labgarcias.ordenes.dto.ArchivoDescarga;
import com.labgarcias.ordenes.dto.OrdenArchivoResponse;
import com.labgarcias.ordenes.service.OrdenArchivoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Órdenes")
public class OrdenArchivoController {

    private static final Set<String> ROLES_ADMINISTRACION = Set.of("ROLE_ADMIN", "ROLE_SUPERADMIN");

    private final OrdenArchivoService ordenArchivoService;

    public OrdenArchivoController(OrdenArchivoService ordenArchivoService) {
        this.ordenArchivoService = ordenArchivoService;
    }

    @PostMapping(value = "/ordenes/{id}/archivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','ODONTOLOGO')")
    @Operation(
            summary = "Adjuntar un archivo a una orden (RN-13, CU-09)",
            description = "D-19: hoy los adjuntos los carga el laboratorio al registrar la orden; el odontólogo "
                    + "solo puede adjuntar a órdenes propias. RN-13: el backend valida formato y tamaño antes de "
                    + "persistir (imágenes JPG/PNG hasta 5 MB, documentos PDF/DOCX hasta 8 MB). "
                    + "RN-01: una orden ajena responde 404, no 403."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Archivo adjuntado"),
            @ApiResponse(responseCode = "404", description = "ORDEN_NO_ENCONTRADA"),
            @ApiResponse(responseCode = "422", description = "ARCHIVO_NO_PERMITIDO")
    })
    public ResponseEntity<OrdenArchivoResponse> adjuntar(@PathVariable Long id,
                                                         @RequestParam("archivo") MultipartFile archivo,
                                                         Authentication authentication) {
        OrdenArchivoResponse creado = ordenArchivoService.adjuntar(
                id, archivo, usuarioId(authentication), esAdministrador(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/ordenes/{id}/archivos")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','ODONTOLOGO')")
    @Operation(
            summary = "Listar los adjuntos de una orden (RN-13, CU-04)",
            description = "Devuelve solo metadatos. RN-01: una orden ajena responde 404."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de adjuntos"),
            @ApiResponse(responseCode = "404", description = "ORDEN_NO_ENCONTRADA")
    })
    public ResponseEntity<List<OrdenArchivoResponse>> listar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ordenArchivoService.listar(
                id, usuarioId(authentication), esAdministrador(authentication)));
    }

    @GetMapping("/archivos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','ODONTOLOGO')")
    @Operation(
            summary = "Descargar un adjunto (RN-13, CU-04)",
            description = "RN-01: verifica la propiedad de la orden dueña del archivo antes de entregarlo; "
                    + "un odontólogo que pide el adjunto de una orden ajena recibe 404."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contenido del archivo"),
            @ApiResponse(responseCode = "404", description = "ARCHIVO_NO_ENCONTRADO / ORDEN_NO_ENCONTRADA")
    })
    public ResponseEntity<org.springframework.core.io.Resource> descargar(@PathVariable Long id,
                                                                          Authentication authentication) {
        ArchivoDescarga descarga = ordenArchivoService.descargar(
                id, usuarioId(authentication), esAdministrador(authentication));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(descarga.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + descarga.nombreOriginal() + "\"")
                .body(descarga.contenido());
    }

    @DeleteMapping("/archivos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(
            summary = "Eliminar un adjunto (§5.2)",
            description = "Borrado definitivo del registro y del binario, para resolver un archivo cargado por "
                    + "error. Solo el laboratorio: el odontólogo no puede borrar adjuntos ni siquiera de sus "
                    + "propias órdenes, así que recibe 403 (no es un caso de RN-01: se le niega por rol)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Archivo eliminado"),
            @ApiResponse(responseCode = "403", description = "SIN_PERMISO"),
            @ApiResponse(responseCode = "404", description = "ARCHIVO_NO_ENCONTRADO")
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ordenArchivoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private Long usuarioId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    private boolean esAdministrador(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLES_ADMINISTRACION::contains);
    }
}
