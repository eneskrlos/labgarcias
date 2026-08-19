package com.labgarcias.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.labgarcias.seguridad.domain.EstadoCuenta;
import com.labgarcias.seguridad.domain.Rol;
import com.labgarcias.seguridad.domain.RolCodigo;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.repository.UsuarioRepository;
import com.labgarcias.shared.excepcion.ReglaNegocioException;

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
}
