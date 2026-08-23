package com.labgarcias.ordenes.dto;

import org.springframework.core.io.Resource;

/**
 * Binario más los metadatos que el controller necesita para armar la respuesta
 * (Content-Type y nombre de descarga). No se serializa a JSON.
 */
public record ArchivoDescarga(Resource contenido, String nombreOriginal, String tipoMime) {
}
