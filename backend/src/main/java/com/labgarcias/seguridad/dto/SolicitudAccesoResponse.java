package com.labgarcias.seguridad.dto;

import java.time.OffsetDateTime;

import com.labgarcias.seguridad.domain.SolicitudAcceso;

import io.swagger.v3.oas.annotations.media.Schema;

/** §3.1.b: una solicitud tal como la ve el administrador en su listado. */
public record SolicitudAccesoResponse(

        @Schema(example = "12")
        Long id,

        @Schema(example = "Dr. Juan Pérez")
        String nombreCompleto,

        @Schema(example = "juan@mail.com")
        String correo,

        @Schema(example = "Av. 18 de Julio 1234")
        String direccion,

        @Schema(example = "+59891234567")
        String telefono,

        @Schema(description = "PENDIENTE, APROBADA o RECHAZADA (D-17)", example = "PENDIENTE")
        String estado,

        OffsetDateTime fechaCreacion,

        @Schema(description = "Nula mientras la solicitud sigue pendiente")
        OffsetDateTime fechaResolucion

) {
    public static SolicitudAccesoResponse de(SolicitudAcceso solicitud) {
        return new SolicitudAccesoResponse(
                solicitud.getId(),
                solicitud.getNombreCompleto(),
                solicitud.getCorreo(),
                solicitud.getDireccion(),
                solicitud.getTelefono(),
                solicitud.getEstado().name(),
                solicitud.getFechaCreacion(),
                solicitud.getFechaResolucion());
    }
}
