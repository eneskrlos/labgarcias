package com.labgarcias.ordenes.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * PATRÓN: Adapter
 * PROBLEMA: llevar el puerto AlmacenamientoArchivos al sistema de archivos local,
 *           que es el único destino disponible en una instalación por laboratorio.
 * MOTIVADO POR: RN-13, D-16 (una instalación por laboratorio, sin servicios externos).
 */
@Component
public class AlmacenamientoLocal implements AlmacenamientoArchivos {

    private final Path directorioBase;

    public AlmacenamientoLocal(@Value("${app.almacenamiento.directorio}") String directorio) {
        this.directorioBase = Paths.get(directorio).toAbsolutePath().normalize();
    }

    /**
     * El nombre físico es un UUID sin extensión: el nombre que subió el usuario nunca
     * toca el sistema de archivos, así que no hay forma de escapar del directorio base.
     * El nombre original y el tipo MIME viajan en orden_archivo y se reponen al descargar.
     */
    @Override
    public String guardar(MultipartFile archivo, Long ordenId) {
        Path carpeta = directorioBase.resolve(String.valueOf(ordenId));
        Path destino = carpeta.resolve(UUID.randomUUID().toString());
        try {
            Files.createDirectories(carpeta);
            archivo.transferTo(destino);
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo guardar el adjunto de la orden " + ordenId, excepcion);
        }
        return directorioBase.relativize(destino).toString();
    }

    @Override
    public Resource cargar(String rutaAlmacenamiento) {
        Path archivo = rutaSegura(rutaAlmacenamiento);
        Resource recurso = new FileSystemResource(archivo);
        if (!recurso.exists()) {
            throw new IllegalStateException("El adjunto ya no está en el almacenamiento: " + rutaAlmacenamiento);
        }
        return recurso;
    }

    /**
     * Si el borrado falla, la excepción tumba la transacción y el registro no se
     * elimina: base y disco quedan consistentes. `deleteIfExists` no falla cuando
     * el archivo ya no está, que es el caso benigno.
     */
    @Override
    public void eliminar(String rutaAlmacenamiento) {
        try {
            Files.deleteIfExists(rutaSegura(rutaAlmacenamiento));
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo borrar el adjunto: " + rutaAlmacenamiento, excepcion);
        }
    }

    private Path rutaSegura(String rutaAlmacenamiento) {
        Path archivo = directorioBase.resolve(rutaAlmacenamiento).normalize();
        if (!archivo.startsWith(directorioBase)) {
            throw new IllegalArgumentException("La ruta del adjunto queda fuera del directorio de almacenamiento.");
        }
        return archivo;
    }
}
