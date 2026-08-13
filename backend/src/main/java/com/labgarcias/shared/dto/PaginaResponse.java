package com.labgarcias.shared.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/** spec.md §1: envoltorio uniforme de toda respuesta paginada del backend. */
public record PaginaResponse<T>(

        List<T> contenido,

        @Schema(example = "45")
        long total,

        @Schema(description = "Base 0", example = "0")
        int pagina,

        @Schema(example = "10")
        int tamano,

        @Schema(example = "5")
        int totalPaginas

) {
    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(
                pagina.getContent(),
                pagina.getTotalElements(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalPages());
    }
}
