package com.labgarcias.shared.util;

import java.util.Set;

import com.labgarcias.shared.excepcion.ValidacionException;

/** spec.md §1: toda vista paginada del sistema comparte la misma validación de tamaño. */
public final class ValidadorPaginacion {

    private static final Set<Integer> TAMANOS_VALIDOS = Set.of(10, 20, 30);

    private ValidadorPaginacion() {
    }

    public static void validarTamano(int size) {
        if (!TAMANOS_VALIDOS.contains(size)) {
            throw new ValidacionException("TAMANO_PAGINA_INVALIDO", "El tamaño de página debe ser 10, 20 o 30.", "size");
        }
    }
}
