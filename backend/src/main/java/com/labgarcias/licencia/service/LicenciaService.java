package com.labgarcias.licencia.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.licencia.domain.EstadoLicencia;
import com.labgarcias.licencia.domain.Licencia;
import com.labgarcias.licencia.dto.LicenciaResponse;
import com.labgarcias.licencia.dto.LicenciaVigenteResponse;
import com.labgarcias.licencia.dto.RegistrarLicenciaRequest;
import com.labgarcias.licencia.repository.LicenciaRepository;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.shared.dto.PaginaResponse;
import com.labgarcias.shared.excepcion.ReglaNegocioException;
import com.labgarcias.shared.util.ValidadorPaginacion;

import jakarta.persistence.EntityManager;

/** RN-20/CU-23: períodos de licencia de esta instalación (D-16: sin multi-tenant). */
@Service
public class LicenciaService {

    private final LicenciaRepository licenciaRepository;
    private final EntityManager entityManager;

    public LicenciaService(LicenciaRepository licenciaRepository, EntityManager entityManager) {
        this.licenciaRepository = licenciaRepository;
        this.entityManager = entityManager;
    }

    /**
     * CU-23: el histórico de períodos, más reciente primero.
     *
     * **Paginado desde T-35** (§8.1 Regla 2): `/admin/licencias` es una tabla de administración
     * como las de órdenes, odontólogos, solicitudes y tipos de trabajo, y §8.1 se aplica a todas
     * "sin excepción" para que se vean y se operen igual. Que una instalación tenga pocos períodos
     * no lo cambia: la regla es de uniformidad, no de rendimiento.
     *
     * `obtenerVigente` **no se paginó**: devuelve un único registro.
     */
    @Transactional(readOnly = true)
    public PaginaResponse<LicenciaResponse> listarHistorico(Pageable pageable) {
        ValidadorPaginacion.validarTamano(pageable.getPageSize());
        return PaginaResponse.de(
                licenciaRepository.findAllByOrderByFechaRegistroDesc(pageable).map(this::aRespuesta));
    }

    @Transactional(readOnly = true)
    public LicenciaVigenteResponse obtenerVigente() {
        return buscarVigente()
                .map(licencia -> new LicenciaVigenteResponse(true, aRespuesta(licencia)))
                .orElseGet(() -> new LicenciaVigenteResponse(false, null));
    }

    @Transactional
    public LicenciaResponse registrar(RegistrarLicenciaRequest request, Long activadaPorId) {
        validarFechas(request);

        Licencia licencia = new Licencia();
        licencia.setFechaInicio(request.fechaInicio());
        licencia.setFechaVencimiento(request.fechaVencimiento());
        licencia.setEstado(EstadoLicencia.ACTIVA);
        licencia.setActivadaPor(entityManager.getReference(Usuario.class, activadaPorId));
        licencia.setObservacion(request.observacion());
        licencia.setFechaRegistro(OffsetDateTime.now());
        return aRespuesta(licenciaRepository.save(licencia));
    }

    private void validarFechas(RegistrarLicenciaRequest request) {
        if (!request.fechaVencimiento().isAfter(request.fechaInicio())) {
            throw new ReglaNegocioException("FECHAS_LICENCIA_INVALIDAS",
                    "La fecha de vencimiento debe ser posterior a la fecha de inicio.", "fechaVencimiento");
        }
    }

    private Optional<Licencia> buscarVigente() {
        LocalDate hoy = LocalDate.now();
        return licenciaRepository
                .findByEstadoAndFechaInicioLessThanEqualAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoDesc(
                        EstadoLicencia.ACTIVA, hoy, hoy)
                .stream()
                .findFirst();
    }

    private LicenciaResponse aRespuesta(Licencia licencia) {
        String activadaPorNombre = licencia.getActivadaPor() != null
                ? licencia.getActivadaPor().getNombreCompleto()
                : null;
        return new LicenciaResponse(
                licencia.getId(), licencia.getFechaInicio(), licencia.getFechaVencimiento(),
                licencia.getEstado().name(), activadaPorNombre, licencia.getFechaRegistro(), licencia.getObservacion());
    }
}
