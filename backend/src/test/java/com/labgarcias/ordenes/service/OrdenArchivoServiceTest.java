package com.labgarcias.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

import com.labgarcias.ordenes.domain.CategoriaArchivo;
import com.labgarcias.ordenes.domain.Orden;
import com.labgarcias.ordenes.domain.OrdenArchivo;
import com.labgarcias.ordenes.dto.ArchivoDescarga;
import com.labgarcias.ordenes.dto.OrdenArchivoResponse;
import com.labgarcias.ordenes.repository.OrdenArchivoRepository;
import com.labgarcias.ordenes.repository.OrdenRepository;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;
import com.labgarcias.shared.excepcion.ReglaNegocioException;
import com.labgarcias.shared.util.ConstantesDominio;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrdenArchivoServiceTest {

    private static final long ID_ORDEN = 1L;
    private static final long ID_DUENO = 7L;
    private static final long ID_INTRUSO = 99L;

    @Mock
    private OrdenRepository ordenRepository;
    @Mock
    private OrdenArchivoRepository archivoRepository;
    @Mock
    private AlmacenamientoArchivos almacenamiento;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private OrdenArchivoService ordenArchivoService;

    private Orden orden;

    @BeforeEach
    void prepararOrdenDelDueno() {
        Usuario dueno = mock(Usuario.class);
        when(dueno.getId()).thenReturn(ID_DUENO);
        orden = mock(Orden.class);
        when(orden.getId()).thenReturn(ID_ORDEN);
        when(orden.getOdontologo()).thenReturn(dueno);

        when(ordenRepository.findById(ID_ORDEN)).thenReturn(Optional.of(orden));
        when(entityManager.getReference(eq(Usuario.class), any())).thenReturn(mock(Usuario.class));
        when(almacenamiento.guardar(any(), anyLong())).thenReturn("1/uuid-del-archivo");
        when(archivoRepository.saveAndFlush(any(OrdenArchivo.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MockMultipartFile archivo(String nombre, String tipoMime, int bytes) {
        return new MockMultipartFile("archivo", nombre, tipoMime, new byte[bytes]);
    }

    @Test
    void unaImagenValidaSeAdjuntaComoCategoriaImagen() {
        OrdenArchivoResponse respuesta = ordenArchivoService.adjuntar(
                ID_ORDEN, archivo("radiografia.jpg", "image/jpeg", 1024), ID_DUENO, false);

        assertThat(respuesta.categoria()).isEqualTo("IMAGEN");
        assertThat(respuesta.nombreOriginal()).isEqualTo("radiografia.jpg");
        assertThat(respuesta.tamanoBytes()).isEqualTo(1024L);
    }

    @Test
    void unDocumentoValidoSeAdjuntaComoCategoriaDocumento() {
        OrdenArchivoResponse respuesta = ordenArchivoService.adjuntar(
                ID_ORDEN, archivo("indicaciones.pdf", "application/pdf", 2048), ID_DUENO, false);

        assertThat(respuesta.categoria()).isEqualTo("DOCUMENTO");
    }

    /** §5.2 criterio 1: el backend rechaza aunque el frontend lo haya permitido. */
    @Test
    void criterio1UnJpgDe6MbEsRechazadoPorElBackend() {
        int seisMegas = 6 * 1024 * 1024;
        assertThat(seisMegas).isGreaterThan((int) ConstantesDominio.TAMANO_MAXIMO_IMAGEN);

        assertThatThrownBy(() -> ordenArchivoService.adjuntar(
                ID_ORDEN, archivo("grande.jpg", "image/jpeg", seisMegas), ID_DUENO, false))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo()).isEqualTo("ARCHIVO_NO_PERMITIDO"));

        verify(almacenamiento, never()).guardar(any(), anyLong());
    }

    /** §5.2 criterio 2. */
    @Test
    void criterio2UnGifEsRechazado() {
        assertThatThrownBy(() -> ordenArchivoService.adjuntar(
                ID_ORDEN, archivo("animacion.gif", "image/gif", 1024), ID_DUENO, false))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo()).isEqualTo("ARCHIVO_NO_PERMITIDO"));

        verify(almacenamiento, never()).guardar(any(), anyLong());
    }

    @Test
    void unPdfDe9MbEsRechazadoPorSuperarElTopeDeDocumento() {
        int nueveMegas = 9 * 1024 * 1024;
        assertThat(nueveMegas).isGreaterThan((int) ConstantesDominio.TAMANO_MAXIMO_DOCUMENTO);

        assertThatThrownBy(() -> ordenArchivoService.adjuntar(
                ID_ORDEN, archivo("enorme.pdf", "application/pdf", nueveMegas), ID_DUENO, false))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void unaImagenDe5MbExactosSeAcepta() {
        OrdenArchivoResponse respuesta = ordenArchivoService.adjuntar(
                ID_ORDEN, archivo("justo.png", "image/png", (int) ConstantesDominio.TAMANO_MAXIMO_IMAGEN),
                ID_DUENO, false);

        assertThat(respuesta.categoria()).isEqualTo("IMAGEN");
    }

    /** Sin Content-Type no hay formato que validar: 422, no el NPE que daba List.of.contains(null). */
    @Test
    void unArchivoSinTipoDeContenidoEsRechazadoConCodigoDeNegocio() {
        MockMultipartFile sinTipo = new MockMultipartFile("archivo", "misterio.bin", null, new byte[16]);

        assertThatThrownBy(() -> ordenArchivoService.adjuntar(ID_ORDEN, sinTipo, ID_DUENO, false))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo()).isEqualTo("ARCHIVO_NO_PERMITIDO"));

        verify(almacenamiento, never()).guardar(any(), anyLong());
    }

    /** nombre_original es NOT NULL: se rechaza antes de guardar el binario, no en el insert. */
    @Test
    void unArchivoSinNombreEsRechazadoAntesDeGuardarElBinario() {
        MockMultipartFile sinNombre = new MockMultipartFile("archivo", null, "image/jpeg", new byte[16]);

        assertThatThrownBy(() -> ordenArchivoService.adjuntar(ID_ORDEN, sinNombre, ID_DUENO, false))
                .isInstanceOf(ReglaNegocioException.class);

        verify(almacenamiento, never()).guardar(any(), anyLong());
        verify(archivoRepository, never()).saveAndFlush(any());
    }

    @Test
    void unArchivoVacioEsRechazado() {
        assertThatThrownBy(() -> ordenArchivoService.adjuntar(
                ID_ORDEN, archivo("vacio.jpg", "image/jpeg", 0), ID_DUENO, false))
                .isInstanceOf(ReglaNegocioException.class);
    }

    /** RN-13: el binario no va a la base; orden_archivo solo guarda la ruta que devuelve el puerto. */
    @Test
    void rn13ElRegistroGuardaLaRutaDelPuertoNoElBinario() {
        ordenArchivoService.adjuntar(ID_ORDEN, archivo("foto.png", "image/png", 512), ID_DUENO, false);

        ArgumentCaptor<OrdenArchivo> captor = ArgumentCaptor.forClass(OrdenArchivo.class);
        verify(archivoRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRutaAlmacenamiento()).isEqualTo("1/uuid-del-archivo");
        assertThat(captor.getValue().getCategoria()).isEqualTo(CategoriaArchivo.IMAGEN);
    }

    /** RN-01: orden ajena → 404, nunca 403. */
    @Test
    void rn01UnOdontologoNoPuedeAdjuntarAUnaOrdenAjena() {
        assertThatThrownBy(() -> ordenArchivoService.adjuntar(
                ID_ORDEN, archivo("foto.jpg", "image/jpeg", 512), ID_INTRUSO, false))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getHttpStatus().value()).isEqualTo(404));

        verify(almacenamiento, never()).guardar(any(), anyLong());
    }

    @Test
    void rn01UnOdontologoNoPuedeListarLosAdjuntosDeUnaOrdenAjena() {
        assertThatThrownBy(() -> ordenArchivoService.listar(ID_ORDEN, ID_INTRUSO, false))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    /** §5.2 criterio 3. */
    @Test
    void criterio3UnOdontologoNoPuedeDescargarElArchivoDeUnaOrdenAjena() {
        OrdenArchivo archivo = new OrdenArchivo();
        archivo.setOrden(orden);
        archivo.setRutaAlmacenamiento("1/uuid-del-archivo");
        when(archivoRepository.findById(5L)).thenReturn(Optional.of(archivo));

        assertThatThrownBy(() -> ordenArchivoService.descargar(5L, ID_INTRUSO, false))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getHttpStatus().value()).isEqualTo(404));

        verify(almacenamiento, never()).cargar(any());
    }

    @Test
    void elDuenoSiPuedeDescargarSuArchivo() {
        OrdenArchivo archivo = new OrdenArchivo();
        archivo.setOrden(orden);
        archivo.setNombreOriginal("radiografia.jpg");
        archivo.setTipoMime("image/jpeg");
        archivo.setRutaAlmacenamiento("1/uuid-del-archivo");
        when(archivoRepository.findById(5L)).thenReturn(Optional.of(archivo));
        when(almacenamiento.cargar("1/uuid-del-archivo")).thenReturn(new ByteArrayResource(new byte[10]));

        ArchivoDescarga descarga = ordenArchivoService.descargar(5L, ID_DUENO, false);

        assertThat(descarga.nombreOriginal()).isEqualTo("radiografia.jpg");
        assertThat(descarga.tipoMime()).isEqualTo("image/jpeg");
    }

    /** D-19: el laboratorio carga los adjuntos de cualquier orden. */
    @Test
    void d19ElAdministradorPuedeAdjuntarAUnaOrdenDeCualquierOdontologo() {
        OrdenArchivoResponse respuesta = ordenArchivoService.adjuntar(
                ID_ORDEN, archivo("molde.pdf", "application/pdf", 4096), ID_INTRUSO, true);

        assertThat(respuesta.categoria()).isEqualTo("DOCUMENTO");
    }

    /** §5.2 criterio 4: se van el registro y el binario. */
    @Test
    void criterio4EliminarBorraElRegistroYElBinario() {
        OrdenArchivo archivo = new OrdenArchivo();
        archivo.setOrden(orden);
        archivo.setRutaAlmacenamiento("1/uuid-del-archivo");
        when(archivoRepository.findById(5L)).thenReturn(Optional.of(archivo));

        ordenArchivoService.eliminar(5L);

        verify(archivoRepository).delete(archivo);
        verify(almacenamiento).eliminar("1/uuid-del-archivo");
    }

    @Test
    void eliminarUnArchivoInexistenteDevuelve404() {
        when(archivoRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenArchivoService.eliminar(404L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getHttpStatus().value()).isEqualTo(404));

        verify(almacenamiento, never()).eliminar(any());
    }

    @Test
    void unaOrdenInexistenteDevuelve404() {
        when(ordenRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenArchivoService.listar(404L, ID_DUENO, false))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
