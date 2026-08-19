package com.labgarcias.ordenes.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

/** RN-13/§5.2: adjuntos de la orden. El binario va al almacenamiento; acá solo metadatos. */
@Service
public class OrdenArchivoService {

    private static final String CODIGO_ARCHIVO_NO_PERMITIDO = "ARCHIVO_NO_PERMITIDO";

    private final OrdenRepository ordenRepository;
    private final OrdenArchivoRepository archivoRepository;
    private final AlmacenamientoArchivos almacenamiento;
    private final EntityManager entityManager;

    public OrdenArchivoService(OrdenRepository ordenRepository,
                               OrdenArchivoRepository archivoRepository,
                               AlmacenamientoArchivos almacenamiento,
                               EntityManager entityManager) {
        this.ordenRepository = ordenRepository;
        this.archivoRepository = archivoRepository;
        this.almacenamiento = almacenamiento;
        this.entityManager = entityManager;
    }

    @Transactional
    public OrdenArchivoResponse adjuntar(Long ordenId, MultipartFile archivo, Long usuarioId, boolean esAdministrador) {
        Orden orden = buscarOrdenAccesible(ordenId, usuarioId, esAdministrador);
        CategoriaArchivo categoria = validarFormatoYTamano(archivo);

        OrdenArchivo registro = new OrdenArchivo();
        registro.setOrden(orden);
        registro.setCategoria(categoria);
        registro.setNombreOriginal(archivo.getOriginalFilename());
        registro.setTipoMime(archivo.getContentType());
        registro.setTamanoBytes(archivo.getSize());
        registro.setSubidoPor(entityManager.getReference(Usuario.class, usuarioId));
        registro.setRutaAlmacenamiento(almacenamiento.guardar(archivo, ordenId));

        // saveAndFlush: fecha_carga la sella la base al insertar.
        return aRespuesta(archivoRepository.saveAndFlush(registro));
    }

    @Transactional(readOnly = true)
    public List<OrdenArchivoResponse> listar(Long ordenId, Long usuarioId, boolean esAdministrador) {
        buscarOrdenAccesible(ordenId, usuarioId, esAdministrador);
        return archivoRepository.findByOrdenIdOrderByFechaCargaAsc(ordenId).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArchivoDescarga descargar(Long archivoId, Long usuarioId, boolean esAdministrador) {
        OrdenArchivo archivo = archivoRepository.findById(archivoId)
                .orElseThrow(this::archivoNoEncontrado);
        buscarOrdenAccesible(archivo.getOrden().getId(), usuarioId, esAdministrador);
        return new ArchivoDescarga(
                almacenamiento.cargar(archivo.getRutaAlmacenamiento()),
                archivo.getNombreOriginal(),
                archivo.getTipoMime());
    }

    /**
     * §5.2: borrado definitivo de un adjunto cargado por error. Solo ADMIN y SUPERADMIN
     * (lo impone la autorización del endpoint), así que no hay verificación de propiedad:
     * el laboratorio opera sobre las órdenes de todos los odontólogos.
     * Se borra primero el registro y después el binario: si el borrado en disco falla,
     * la transacción se deshace y ni la fila ni el archivo desaparecen.
     */
    @Transactional
    public void eliminar(Long archivoId) {
        OrdenArchivo archivo = archivoRepository.findById(archivoId)
                .orElseThrow(this::archivoNoEncontrado);
        archivoRepository.delete(archivo);
        almacenamiento.eliminar(archivo.getRutaAlmacenamiento());
    }

    /** RN-01: una orden ajena se responde 404, no 403, para no revelar que existe. */
    private Orden buscarOrdenAccesible(Long ordenId, Long usuarioId, boolean esAdministrador) {
        Orden orden = ordenRepository.findById(ordenId).orElseThrow(this::ordenNoEncontrada);
        if (!esAdministrador && !orden.getOdontologo().getId().equals(usuarioId)) {
            throw ordenNoEncontrada();
        }
        return orden;
    }

    /** RN-13: el backend valida siempre, sin confiar en lo que haya filtrado el frontend. */
    private CategoriaArchivo validarFormatoYTamano(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException(CODIGO_ARCHIVO_NO_PERMITIDO, "El archivo está vacío.", "archivo");
        }
        String tipoMime = archivo.getContentType();
        if (ConstantesDominio.FORMATOS_IMAGEN.contains(tipoMime)) {
            validarTamano(archivo.getSize(), ConstantesDominio.TAMANO_MAXIMO_IMAGEN, "5 MB");
            return CategoriaArchivo.IMAGEN;
        }
        if (ConstantesDominio.FORMATOS_DOCUMENTO.contains(tipoMime)) {
            validarTamano(archivo.getSize(), ConstantesDominio.TAMANO_MAXIMO_DOCUMENTO, "8 MB");
            return CategoriaArchivo.DOCUMENTO;
        }
        throw new ReglaNegocioException(CODIGO_ARCHIVO_NO_PERMITIDO,
                "Formato no permitido. Se aceptan imágenes JPG o PNG y documentos PDF o DOCX.", "archivo");
    }

    private void validarTamano(long tamanoBytes, long maximo, String maximoLegible) {
        if (tamanoBytes > maximo) {
            throw new ReglaNegocioException(CODIGO_ARCHIVO_NO_PERMITIDO,
                    "El archivo supera el tamaño máximo permitido de " + maximoLegible + ".", "archivo");
        }
    }

    private OrdenArchivoResponse aRespuesta(OrdenArchivo archivo) {
        return new OrdenArchivoResponse(
                archivo.getId(),
                archivo.getNombreOriginal(),
                archivo.getCategoria().name(),
                archivo.getTipoMime(),
                archivo.getTamanoBytes(),
                archivo.getFechaCarga());
    }

    private RecursoNoEncontradoException ordenNoEncontrada() {
        return new RecursoNoEncontradoException("ORDEN_NO_ENCONTRADA", "No existe la orden solicitada.");
    }

    private RecursoNoEncontradoException archivoNoEncontrado() {
        return new RecursoNoEncontradoException("ARCHIVO_NO_ENCONTRADO", "No existe el archivo solicitado.");
    }
}
