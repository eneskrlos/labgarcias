package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.labgarcias.notificaciones.domain.TipoEvento;
import com.labgarcias.seguridad.domain.SolicitudAccesoEvent;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class SolicitudAccesoNotificacionListenerTest {

    private static final long ADMIN = 1L;
    private static final long SUPERADMIN = 2L;

    @Mock
    private NotificacionService notificacionService;
    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private SolicitudAccesoNotificacionListener listener;

    private final SolicitudAccesoEvent evento = new SolicitudAccesoEvent(12L, "Dr. Juan Pérez");

    /** Los mocks se arman antes del when(): construirlos dentro de thenReturn() interrumpe el stubbing. */
    private void hayDosAdministradoresActivos() {
        Usuario admin = mock(Usuario.class);
        Usuario superadmin = mock(Usuario.class);
        when(admin.getId()).thenReturn(ADMIN);
        when(superadmin.getId()).thenReturn(SUPERADMIN);
        when(usuarioService.listarAdministradoresActivosParaNotificacion()).thenReturn(List.of(admin, superadmin));
    }

    /** §6.2: SOLICITUD_ACCESO va al Administrador, que es cada cuenta de administración activa. */
    @Test
    void avisaAcadaAdministradorActivo() {
        hayDosAdministradoresActivos();

        listener.alRecibirseUnaSolicitud(evento);

        verify(notificacionService).registrar(eq(ADMIN), eq(TipoEvento.SOLICITUD_ACCESO),
                eq("Nueva solicitud de acceso de Dr. Juan Pérez."), isNull());
        verify(notificacionService).registrar(eq(SUPERADMIN), eq(TipoEvento.SOLICITUD_ACCESO),
                eq("Nueva solicitud de acceso de Dr. Juan Pérez."), isNull());
    }

    /** Sin cuentas de administración activas no hay a quién avisarle, y no es un error. */
    @Test
    void sinAdministradoresActivosNoRegistraNada() {
        when(usuarioService.listarAdministradoresActivosParaNotificacion()).thenReturn(List.of());

        listener.alRecibirseUnaSolicitud(evento);

        verifyNoInteractions(notificacionService);
    }

    /**
     * Agente.md 5.6: el aviso corre después del commit. Si la solicitud se deshace, no queda
     * notificación de algo que nunca pasó. Se verifica sobre la anotación porque el efecto solo
     * se vería con una transacción real.
     */
    @Test
    void elListenerCorreDespuesDelCommit() {
        Method metodo = Arrays.stream(SolicitudAccesoNotificacionListener.class.getDeclaredMethods())
                .filter(candidato -> candidato.getName().equals("alRecibirseUnaSolicitud"))
                .findFirst()
                .orElseThrow();

        TransactionalEventListener anotacion = metodo.getAnnotation(TransactionalEventListener.class);
        assertThat(anotacion).isNotNull();
        assertThat(anotacion.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
