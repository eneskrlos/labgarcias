package com.labgarcias.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.Rol;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.dto.ActualizarPerfilRequest;
import com.labgarcias.seguridad.dto.PerfilResponse;
import com.labgarcias.seguridad.dto.UsuarioResponse;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.dto.PaginaResponse;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;
import com.labgarcias.shared.excepcion.ReglaNegocioException;
import com.labgarcias.shared.excepcion.ValidacionException;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final long ID = 3L;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    /**
     * Fixture: las stubs van lenient porque los filtros del service cortan en cadena.
     * Si el rol ya descalifica al usuario, nunca se llega a mirar el estado de la cuenta.
     */
    private Usuario usuario(RolCodigo rolCodigo, EstadoCuenta estadoCuenta) {
        Rol rol = mock(Rol.class);
        lenient().when(rol.getCodigo()).thenReturn(rolCodigo);
        Usuario usuario = mock(Usuario.class);
        lenient().when(usuario.getRol()).thenReturn(rol);
        lenient().when(usuario.getEstadoCuenta()).thenReturn(estadoCuenta);
        return usuario;
    }

    /** El mock se arma antes del when(): construirlo dentro de thenReturn() lo interrumpe. */
    private void repositorioDevuelve(RolCodigo rolCodigo, EstadoCuenta estadoCuenta) {
        Usuario encontrado = usuario(rolCodigo, estadoCuenta);
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(encontrado));
    }

    @Test
    void unOdontologoActivoEsValido() {
        repositorioDevuelve(RolCodigo.ODONTOLOGO, EstadoCuenta.ACTIVA);

        assertThat(usuarioService.obtenerOdontologoActivoParaOrden(ID)).isNotNull();
    }

    /** §5.1: registrar una orden a nombre del admin no tiene sentido. */
    @Test
    void unUsuarioDeOtroRolEsRechazado() {
        repositorioDevuelve(RolCodigo.ADMIN, EstadoCuenta.ACTIVA);

        assertThatThrownBy(() -> usuarioService.obtenerOdontologoActivoParaOrden(ID))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo()).isEqualTo("ODONTOLOGO_INVALIDO"));
    }

    @Test
    void unaCuentaDadaDeBajaEsRechazada() {
        repositorioDevuelve(RolCodigo.ODONTOLOGO, EstadoCuenta.INACTIVA);

        assertThatThrownBy(() -> usuarioService.obtenerOdontologoActivoParaOrden(ID))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo()).isEqualTo("ODONTOLOGO_INVALIDO"));
    }

    /** §7: el perfil sale del usuario del token, con el estado de Telegram que pide §6.5. */
    @Test
    void elPerfilLlevaLosDatosPropiosYElEstadoDeTelegram() {
        Usuario encontrado = new Usuario();
        Rol rol = mock(Rol.class);
        when(rol.getCodigo()).thenReturn(RolCodigo.ODONTOLOGO);
        encontrado.setRol(rol);
        encontrado.setNombreCompleto("Dr. Juan Pérez");
        encontrado.setCorreo("juan@mail.com");
        encontrado.vincularTelegram("987654321");
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(encontrado));

        PerfilResponse perfil = usuarioService.obtenerPerfil(ID);

        assertThat(perfil.nombreCompleto()).isEqualTo("Dr. Juan Pérez");
        assertThat(perfil.rol()).isEqualTo("ODONTOLOGO");
        assertThat(perfil.telegramVinculado()).isTrue();
    }

    /** §6.5: el chat es el destino de las notificaciones; no tiene por qué salir en la respuesta. */
    @Test
    void elPerfilNoExponeElChatDeTelegram() {
        assertThat(PerfilResponse.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("telegramChatId");
    }

    /** §6.5 paso 4: la columna es de `usuario`, así que la escribe este servicio y nadie más. */
    @Test
    void vincularGuardaElChatYPrendeLaBandera() {
        Usuario encontrado = new Usuario();
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(encontrado));

        usuarioService.vincularTelegram(ID, "987654321");

        assertThat(encontrado.getTelegramChatId()).isEqualTo("987654321");
        assertThat(encontrado.isTelegramVinculado()).isTrue();
    }

    /** §6.5 criterio 3: desvincular limpia las dos columnas, para que no quede un destino huérfano. */
    @Test
    void desvincularLimpiaElChatYLaBandera() {
        Usuario encontrado = new Usuario();
        encontrado.vincularTelegram("987654321");
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(encontrado));

        usuarioService.desvincularTelegram(ID);

        assertThat(encontrado.getTelegramChatId()).isNull();
        assertThat(encontrado.isTelegramVinculado()).isFalse();
    }

    @Test
    void unUsuarioInexistenteNoTienePerfil() {
        when(usuarioRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPerfil(ID))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getCodigo())
                        .isEqualTo("USUARIO_NO_ENCONTRADO"));
    }

    /**
     * Los tres rechazos comparten código, mensaje y campo: si difirieran, probar ids sueltos
     * revelaría qué cuentas existen y con qué rol.
     */
    @Test
    void losTresRechazosSonIndistinguiblesEntreSi() {
        Usuario admin = usuario(RolCodigo.ADMIN, EstadoCuenta.ACTIVA);
        Usuario inactivo = usuario(RolCodigo.ODONTOLOGO, EstadoCuenta.INACTIVA);
        when(usuarioRepository.findById(404L)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(inactivo));

        assertThat(List.of(404L, 1L, 2L))
                .allSatisfy(id -> assertThatThrownBy(() -> usuarioService.obtenerOdontologoActivoParaOrden(id))
                        .isInstanceOf(ReglaNegocioException.class)
                        .hasMessage("El odontólogo indicado no existe o no está activo.")
                        .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCampo()).isEqualTo("odontologoId")));
    }

    // ---------- T-28: §7, perfil editable ----------

    /** §7: se editan nombre y dirección, y nada más. */
    @Test
    void elPerfilEditableGuardaNombreYDireccion() {
        Usuario encontrado = usuario(RolCodigo.ODONTOLOGO, EstadoCuenta.ACTIVA);
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(encontrado));

        usuarioService.actualizarPerfil(ID, new ActualizarPerfilRequest("Dr. Juan Pérez", "Av. 18 de Julio 1234"));

        verify(encontrado).setNombreCompleto("Dr. Juan Pérez");
        verify(encontrado).setDireccion("Av. 18 de Julio 1234");
    }

    /**
     * §7: **ni el rol ni el correo se pueden cambiar desde el perfil.** Es una guarda estructural:
     * si alguien los agrega al request, este test falla antes de que llegue a producción.
     */
    @Test
    void elPerfilEditableNoPuedeTocarRolNiCorreo() {
        Usuario encontrado = usuario(RolCodigo.ODONTOLOGO, EstadoCuenta.ACTIVA);
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(encontrado));

        usuarioService.actualizarPerfil(ID, new ActualizarPerfilRequest("Otro Nombre", "Otra dirección"));

        verify(encontrado, never()).setRol(any());
        verify(encontrado, never()).setCorreo(any());
        verify(encontrado, never()).setNombreUsuario(any());
        verify(encontrado, never()).setEstadoCuenta(any());
        assertThat(ActualizarPerfilRequest.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("nombreCompleto", "direccion");
    }

    // ---------- T-28: CU-17, mantenimiento del padrón ----------

    @Test
    void cu17ElPadronSeDevuelvePaginado() {
        Pageable pagina = PageRequest.of(0, 10);
        Usuario encontrado = usuario(RolCodigo.ADMIN, EstadoCuenta.ACTIVA);
        when(usuarioRepository.findAllByOrderByNombreCompletoAsc(pagina))
                .thenReturn(new PageImpl<>(List.of(encontrado), pagina, 1));

        PaginaResponse<UsuarioResponse> padron = usuarioService.listarTodos(pagina);

        assertThat(padron.total()).isEqualTo(1);
        assertThat(padron.contenido().get(0).rol()).isEqualTo("ADMIN");
    }

    @Test
    void cu17UnTamanoDePaginaNoPermitidoEsRechazado() {
        assertThatThrownBy(() -> usuarioService.listarTodos(PageRequest.of(0, 15)))
                .isInstanceOf(ValidacionException.class)
                .satisfies(ex -> assertThat(((ValidacionException) ex).getCodigo()).isEqualTo("TAMANO_PAGINA_INVALIDO"));
    }

    @Test
    void cu17DesactivarUnaCuentaLaDejaInactiva() {
        Usuario encontrado = usuario(RolCodigo.ODONTOLOGO, EstadoCuenta.ACTIVA);
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(encontrado));

        usuarioService.cambiarEstado(ID, "INACTIVA", 99L);

        verify(encontrado).setEstadoCuenta(EstadoCuenta.INACTIVA);
    }

    /**
     * CU-17: el SuperAdmin es quien reactiva a los demás. Si pudiera desactivarse a sí mismo y
     * fuera el último activo, el sistema quedaría irrecuperable desde la aplicación.
     */
    @Test
    void cu17NadieCambiaElEstadoDeSuPropiaCuenta() {
        assertThatThrownBy(() -> usuarioService.cambiarEstado(ID, "INACTIVA", ID))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaNegocioException) ex).getCodigo())
                        .isEqualTo("AUTODESACTIVACION_NO_PERMITIDA"));

        verify(usuarioRepository, never()).findById(ID);
    }

    /** D-18 eliminó la verificación: una cuenta ahí quedaría sin forma de destrabarse. */
    @Test
    void cu17NoSePuedeVolverAPendienteDeVerificacion() {
        assertThatThrownBy(() -> usuarioService.cambiarEstado(ID, "PENDIENTE_VERIFICACION", 99L))
                .isInstanceOf(ValidacionException.class)
                .satisfies(ex -> assertThat(((ValidacionException) ex).getCodigo()).isEqualTo("ESTADO_CUENTA_INVALIDO"));
    }

    /** Un estado inexistente da 400 con su código, no un 500 del binding. */
    @Test
    void cu17UnEstadoInexistenteEsRechazadoConSuCodigo() {
        assertThatThrownBy(() -> usuarioService.cambiarEstado(ID, "SUSPENDIDA", 99L))
                .isInstanceOf(ValidacionException.class)
                .satisfies(ex -> assertThat(((ValidacionException) ex).getCodigo()).isEqualTo("ESTADO_CUENTA_INVALIDO"));
    }

    /** CU-17/§8.2: el padrón no expone contraseñas ni el chat de Telegram. */
    @Test
    void cu17ElPadronNoExponeSecretos() {
        assertThat(UsuarioResponse.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("id", "nombreCompleto", "correo", "nombreUsuario", "rol", "estadoCuenta");
    }
}
