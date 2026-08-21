package com.labgarcias.seguridad.service;

import java.time.OffsetDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.seguridad.domain.EstadoSolicitud;
import com.labgarcias.seguridad.domain.SolicitudAcceso;
import com.labgarcias.seguridad.domain.SolicitudAccesoEvent;
import com.labgarcias.seguridad.dto.SolicitudAccesoRequest;
import com.labgarcias.seguridad.dto.SolicitudAccesoResponse;
import com.labgarcias.seguridad.repository.SolicitudAccesoRepository;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.dto.PaginaResponse;
import com.labgarcias.shared.excepcion.ConflictoException;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;
import com.labgarcias.shared.excepcion.ValidacionException;
import com.labgarcias.shared.util.ValidadorPaginacion;

/**
 * D-17/§3.1: el pedido de acceso de un odontólogo y su gestión por el administrador.
 *
 * Este servicio **no crea usuarios**: el alta de la cuenta es §3.1.b y es la que además pasa la
 * solicitud a APROBADA. Acá una solicitud solo nace PENDIENTE o muere RECHAZADA.
 */
@Service
public class SolicitudAccesoService {

    private final SolicitudAccesoRepository solicitudAccesoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher publicadorEventos;

    public SolicitudAccesoService(SolicitudAccesoRepository solicitudAccesoRepository,
                                  UsuarioRepository usuarioRepository,
                                  ApplicationEventPublisher publicadorEventos) {
        this.solicitudAccesoRepository = solicitudAccesoRepository;
        this.usuarioRepository = usuarioRepository;
        this.publicadorEventos = publicadorEventos;
    }

    /**
     * §3.1: registra la solicitud y avisa al administrador. Criterio 1: no se crea ningún usuario
     * ni nada que habilite un login.
     *
     * El evento se publica dentro de la transacción, pero su listener corre AFTER_COMMIT
     * (Agente.md §5.6): si el registro se deshace, no queda ninguna notificación de algo que
     * nunca pasó.
     */
    @Transactional
    public SolicitudAcceso registrar(SolicitudAccesoRequest request) {
        validarCorreoDisponible(request.correo());

        SolicitudAcceso solicitud = new SolicitudAcceso();
        solicitud.setNombreCompleto(request.nombreCompleto());
        solicitud.setCorreo(request.correo());
        solicitud.setDireccion(request.direccion());
        solicitud.setTelefono(request.telefono());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        SolicitudAcceso persistida = solicitudAccesoRepository.save(solicitud);

        publicadorEventos.publishEvent(new SolicitudAccesoEvent(persistida.getId(), persistida.getNombreCompleto()));
        return persistida;
    }

    /** §3.1.b: listado del administrador, con filtro opcional por estado. */
    @Transactional(readOnly = true)
    public PaginaResponse<SolicitudAccesoResponse> listarPaginado(Pageable pageable, String estado) {
        ValidadorPaginacion.validarTamano(pageable.getPageSize());
        EstadoSolicitud filtro = estadoDesdeParametro(estado);
        Page<SolicitudAcceso> pagina = filtro == null
                ? solicitudAccesoRepository.findAll(pageable)
                : solicitudAccesoRepository.findByEstado(filtro, pageable);
        return PaginaResponse.de(pagina.map(SolicitudAccesoResponse::de));
    }

    /**
     * §3.1.b: el administrador descarta una solicitud. No se notifica al solicitante: §6.2 no
     * tiene un evento para esto y no se inventan eventos.
     *
     * Rechazar no bloquea el correo: §3.1 solo impide duplicar una solicitud **pendiente**, así
     * que quien fue rechazado puede volver a solicitar.
     */
    @Transactional
    public SolicitudAccesoResponse rechazar(Long id) {
        SolicitudAcceso solicitud = solicitudAccesoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("SOLICITUD_NO_ENCONTRADA",
                        "La solicitud de acceso no existe."));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ConflictoException("SOLICITUD_YA_RESUELTA",
                    "La solicitud ya fue resuelta y no puede rechazarse.");
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaResolucion(OffsetDateTime.now());
        return SolicitudAccesoResponse.de(solicitud);
    }

    /**
     * El filtro llega como texto libre desde la URL. Se convierte acá, con su propio código de
     * error: dejar que Spring lo convierta haría que un valor cualquiera terminara en el
     * manejador genérico, que responde 500.
     */
    private EstadoSolicitud estadoDesdeParametro(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoSolicitud.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException excepcion) {
            throw new ValidacionException("ESTADO_SOLICITUD_INVALIDO",
                    "El estado debe ser PENDIENTE, APROBADA o RECHAZADA.", "estado");
        }
    }

    /**
     * §3.1.b criterio 4: aprobar una solicitud es crear la cuenta, así que esto lo llama el alta
     * de odontólogo y nadie más. No hay endpoint de aprobación: sin cuenta creada, aprobar no
     * significaría nada.
     *
     * Corre dentro de la transacción del alta: si la creación del usuario falla, la solicitud no
     * queda marcada como aprobada.
     */
    @Transactional
    public void aprobar(Long id) {
        SolicitudAcceso solicitud = solicitudAccesoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("SOLICITUD_NO_ENCONTRADA",
                        "La solicitud de acceso no existe."));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ConflictoException("SOLICITUD_YA_RESUELTA",
                    "La solicitud ya fue resuelta.", "solicitudId");
        }

        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setFechaResolucion(OffsetDateTime.now());
    }

    private void validarCorreoDisponible(String correo) {
        if (usuarioRepository.findByCorreoIgnoreCase(correo).isPresent()) {
            throw new ConflictoException("CORREO_YA_REGISTRADO",
                    "Ya existe una cuenta con ese correo.", "correo");
        }
        if (solicitudAccesoRepository.existsByCorreoIgnoreCaseAndEstado(correo, EstadoSolicitud.PENDIENTE)) {
            throw new ConflictoException("SOLICITUD_YA_EXISTENTE",
                    "Ya hay una solicitud pendiente con ese correo.", "correo");
        }
    }
}
