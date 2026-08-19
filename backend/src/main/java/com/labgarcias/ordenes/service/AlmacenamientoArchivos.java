package com.labgarcias.ordenes.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * PATRÓN: Adapter (puerto)
 * PROBLEMA: el binario de un adjunto no vive en la base (orden_archivo solo guarda
 *           ruta y metadatos), y dónde vive es una decisión de infraestructura que
 *           puede cambiar —hoy disco local, mañana S3— sin que el módulo de órdenes
 *           se entere.
 * MOTIVADO POR: RN-13 (adjuntos de la orden), Agente.md 5.5 (único puerto autorizado
 *               para almacenamiento, con AlmacenamientoLocal como implementación).
 */
public interface AlmacenamientoArchivos {

    /** Persiste el binario y devuelve la ruta relativa que se guarda en orden_archivo. */
    String guardar(MultipartFile archivo, Long ordenId);

    /** Recupera el binario a partir de la ruta relativa almacenada. */
    Resource cargar(String rutaAlmacenamiento);
}
