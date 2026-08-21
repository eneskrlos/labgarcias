package com.labgarcias.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.labgarcias.seguridad.domain.CredencialesCreadasEvent;
import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.ProveedorAuth;
import com.labgarcias.seguridad.domain.Rol;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.dto.CrearOdontologoRequest;
import com.labgarcias.seguridad.dto.OdontologoResponse;
import com.labgarcias.seguridad.repository.RolRepository;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.excepcion.ConflictoException;
import com.labgarcias.shared.excepcion.DominioException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OdontologoServiceTest {

    private static final String PASSWORD_GENERADA = "Ab3$Kd9!Xz2P";
    private static final String CORREO = "juan@mail.com";
    private static final String NOMBRE_USUARIO = "jperez";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private SolicitudAccesoService solicitudAccesoService;
    @Mock
    private GeneradorPassword generadorPassword;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher publicadorEventos;

    @InjectMocks
    private OdontologoService odontologoService;

    private CrearOdontologoRequest request;

    @BeforeEach
    void prepararEscenarioFeliz() {
        request = new CrearOdontologoRequest(
                "Dr. Juan Pérez", CORREO, NOMBRE_USUARIO, "Av. 18 de Julio 1234", "+59891234567", null);

        // Rol tiene constructor protected (lo instancia JPA), así que en el test va un doble.
        Rol rolOdontologo = org.mockito.Mockito.mock(Rol.class);
        when(rolOdontologo.getCodigo()).thenReturn(RolCodigo.ODONTOLOGO);

        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());
        when(usuarioRepository.findByNombreUsuario(NOMBRE_USUARIO)).thenReturn(Optional.empty());
        when(rolRepository.findByCodigo(RolCodigo.ODONTOLOGO)).thenReturn(Optional.of(rolOdontologo));
        when(generadorPassword.generar()).thenReturn(PASSWORD_GENERADA);
        when(passwordEncoder.encode(PASSWORD_GENERADA)).thenReturn("$2a$10$hashBCrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private Usuario usuarioGuardado() {
        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(capturado.capture());
        return capturado.getValue();
    }

    private CredencialesCreadasEvent eventoPublicado() {
        ArgumentCaptor<CredencialesCreadasEvent> capturado =
                ArgumentCaptor.forClass(CredencialesCreadasEvent.class);
        verify(publicadorEventos).publishEvent(capturado.capture());
        return capturado.getValue();
    }

    /** §3.1.b paso 3: rol ODONTOLOGO, cuenta ACTIVA, correo verificado y cambio obligatorio. */
    @Test
    void creaLaCuentaConRolEstadoYBanderaDeCambio() {
        odontologoService.crear(request);

        Usuario creado = usuarioGuardado();
        assertThat(creado.getRol().getCodigo()).isEqualTo(RolCodigo.ODONTOLOGO);
        assertThat(creado.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(creado.isCorreoVerificado()).isTrue();
        assertThat(creado.isDebeCambiarPassword()).isTrue();
        assertThat(creado.getProveedorAuth()).isEqualTo(ProveedorAuth.LOCAL);
        assertThat(creado.getTelefono()).isEqualTo("+59891234567");
    }

    /** §3.1.b criterio 1: a la base va el hash, nunca la contraseña en claro. */
    @Test
    void criterio1LaBaseSoloRecibeElHashDeLaContrasena() {
        odontologoService.crear(request);

        Usuario creado = usuarioGuardado();
        assertThat(creado.getPasswordHash()).isEqualTo("$2a$10$hashBCrypt");
        assertThat(creado.getPasswordHash()).isNotEqualTo(PASSWORD_GENERADA);
        verify(passwordEncoder).encode(PASSWORD_GENERADA);
    }

    /** §3.1.b criterio 1: la contraseña tampoco vuelve en la respuesta del endpoint. */
    @Test
    void criterio1LaRespuestaNoContieneLaContrasena() {
        OdontologoResponse respuesta = odontologoService.crear(request);

        assertThat(respuesta.toString()).doesNotContain(PASSWORD_GENERADA);
        assertThat(respuesta.debeCambiarPassword()).isTrue();
        assertThat(respuesta.estadoCuenta()).isEqualTo("ACTIVA");
    }

    /** §3.1.b paso 4: la contraseña sale solo dentro del evento, para el correo. */
    @Test
    void publicaElEventoConLaContrasenaEnMemoria() {
        odontologoService.crear(request);

        CredencialesCreadasEvent evento = eventoPublicado();
        assertThat(evento.passwordTemporal()).isEqualTo(PASSWORD_GENERADA);
        assertThat(evento.correo()).isEqualTo(CORREO);
        assertThat(evento.nombreUsuario()).isEqualTo(NOMBRE_USUARIO);
    }

    /** §3.1.b criterio 4: crear desde una solicitud la deja APROBADA. */
    @Test
    void criterio4AlCrearDesdeUnaSolicitudLaAprueba() {
        odontologoService.crear(new CrearOdontologoRequest(
                "Dr. Juan Pérez", CORREO, NOMBRE_USUARIO, "Av. 18 de Julio 1234", "+59891234567", 12L));

        verify(solicitudAccesoService).aprobar(12L);
    }

    /** El alta directa (D-17: "o directamente") no toca ninguna solicitud. */
    @Test
    void sinSolicitudIdNoApruebaNada() {
        odontologoService.crear(request);

        verify(solicitudAccesoService, never()).aprobar(anyLong());
    }

    @Test
    void rechazaUnCorreoYaRegistrado() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> odontologoService.crear(request))
                .isInstanceOf(ConflictoException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("CORREO_YA_REGISTRADO");

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(publicadorEventos, never()).publishEvent(any(CredencialesCreadasEvent.class));
    }

    @Test
    void rechazaUnNombreDeUsuarioYaRegistrado() {
        when(usuarioRepository.findByNombreUsuario(NOMBRE_USUARIO)).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> odontologoService.crear(request))
                .isInstanceOf(ConflictoException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("NOMBRE_USUARIO_YA_REGISTRADO");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    /** Los campos opcionales vacíos entran como null, no como cadena vacía. */
    @Test
    void losOpcionalesEnBlancoSeGuardanComoNulos() {
        odontologoService.crear(new CrearOdontologoRequest(
                "Dr. Juan Pérez", CORREO, NOMBRE_USUARIO, "", "", null));

        Usuario creado = usuarioGuardado();
        assertThat(creado.getDireccion()).isNull();
        assertThat(creado.getTelefono()).isNull();
    }

    /** El generador es el único origen de la contraseña: el request no puede traer una. */
    @Test
    void laContrasenaSiempreLaGeneraElSistema() {
        odontologoService.crear(request);

        verify(generadorPassword).generar();
        assertThat(CrearOdontologoRequest.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("password", "passwordTemporal");
    }

    /** Sin el rol en la base, el alta falla explícitamente en vez de guardar una cuenta sin rol. */
    @Test
    void sinRolDeOdontologoConfiguradoFallaConCodigoPropio() {
        when(rolRepository.findByCodigo(RolCodigo.ODONTOLOGO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> odontologoService.crear(request))
                .isInstanceOf(DominioException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("ROL_NO_DISPONIBLE");
    }

    /** El correo se compara sin distinguir mayúsculas, igual que en el login. */
    @Test
    void elCorreoSeComparaSinDistinguirMayusculas() {
        odontologoService.crear(request);

        verify(usuarioRepository).findByCorreoIgnoreCase(anyString());
    }
}
