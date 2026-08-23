package com.labgarcias.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

class AlmacenamientoLocalTest {

    @TempDir
    Path directorioTemporal;

    private AlmacenamientoLocal almacenamiento;

    @BeforeEach
    void prepararAlmacenamiento() {
        almacenamiento = new AlmacenamientoLocal(directorioTemporal.toString());
    }

    private MockMultipartFile archivo(byte[] contenido) {
        return new MockMultipartFile("archivo", "radiografia.jpg", "image/jpeg", contenido);
    }

    @Test
    void guardaElBinarioBajoUnaCarpetaPorOrden() {
        String ruta = almacenamiento.guardar(archivo(new byte[] { 1, 2, 3 }), 42L);

        assertThat(ruta).startsWith("42/");
        assertThat(directorioTemporal.resolve(ruta)).exists();
    }

    /** El nombre que sube el usuario nunca toca el sistema de archivos. */
    @Test
    void elNombreFisicoNoConservaElNombreOriginal() {
        String ruta = almacenamiento.guardar(archivo(new byte[] { 1 }), 42L);

        assertThat(ruta).doesNotContain("radiografia");
    }

    @Test
    void dosArchivosDeLaMismaOrdenNoSePisan() {
        String primera = almacenamiento.guardar(archivo(new byte[] { 1 }), 42L);
        String segunda = almacenamiento.guardar(archivo(new byte[] { 2 }), 42L);

        assertThat(primera).isNotEqualTo(segunda);
        assertThat(directorioTemporal.resolve(primera)).exists();
        assertThat(directorioTemporal.resolve(segunda)).exists();
    }

    @Test
    void cargaDevuelveElContenidoQueSeGuardo() throws IOException {
        byte[] contenido = { 10, 20, 30 };
        String ruta = almacenamiento.guardar(archivo(contenido), 42L);

        Resource recurso = almacenamiento.cargar(ruta);

        assertThat(recurso.exists()).isTrue();
        assertThat(recurso.getContentAsByteArray()).isEqualTo(contenido);
    }

    @Test
    void unaRutaQueEscapaDelDirectorioBaseEsRechazada() {
        assertThatThrownBy(() -> almacenamiento.cargar("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unaRutaInexistenteFallaDeFormaExplicita() {
        assertThatThrownBy(() -> almacenamiento.cargar("42/no-existe"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void eliminarBorraElBinarioDelDisco() {
        String ruta = almacenamiento.guardar(archivo(new byte[] { 1 }), 42L);
        assertThat(directorioTemporal.resolve(ruta)).exists();

        almacenamiento.eliminar(ruta);

        assertThat(directorioTemporal.resolve(ruta)).doesNotExist();
    }

    /** El registro pudo quedar apuntando a un archivo ya inexistente: borrarlo igual debe funcionar. */
    @Test
    void eliminarUnArchivoQueYaNoEstaNoFalla() {
        almacenamiento.eliminar("42/no-existe");
    }

    @Test
    void eliminarUnaRutaQueEscapaDelDirectorioBaseEsRechazado() {
        assertThatThrownBy(() -> almacenamiento.eliminar("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elDirectorioDeLaOrdenSeCreaSiNoExiste() {
        assertThat(Files.exists(directorioTemporal.resolve("77"))).isFalse();

        almacenamiento.guardar(archivo(new byte[] { 1 }), 77L);

        assertThat(Files.isDirectory(directorioTemporal.resolve("77"))).isTrue();
    }
}
