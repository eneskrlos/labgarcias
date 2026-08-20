package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.labgarcias.ordenes.domain.EstadoOrdenCambiadoEvent;
import com.labgarcias.ordenes.domain.OrdenCreadaEvent;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class OrdenNotificacionListenerTest {

    private static final long ORDEN = 12L;
    private static final long DUENO = 3L;
    private static final long ADMIN = 1L;
    private static final long SUPERADMIN = 2L;
    private static final int PACIENTE = 1000;

    @Mock
    private NotificacionService notificacionService;
    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private OrdenNotificacionListener listener;

    private OrdenCreadaEvent ordenCreada(boolean notificaAdmin) {
        return new OrdenCreadaEvent(ORDEN, "LG-0001", DUENO, PACIENTE, notificaAdmin);
    }

    /** Los mocks se arman antes del when(): construirlos dentro de thenReturn() interrumpe el stubbing. */
    private void hayDosAdministradoresActivos() {
        Usuario admin = mock(Usuario.class);
        Usuario superadmin = mock(Usuario.class);
        when(admin.getId()).thenReturn(ADMIN);
        when(superadmin.getId()).thenReturn(SUPERADMIN);
        when(usuarioService.listarAdministradoresActivosParaNotificacion()).thenReturn(List.of(admin, superadmin));
    }

    /** §5.1 paso 10/D-19: la orden la carga el laboratorio, así que al dueño hay que avisarle. */
    @Test
    void paso10AvisaAlOdontologoDuenoQueSuOrdenQuedoRegistrada() {
        listener.alRegistrarseUnaOrden(ordenCreada(false));

        verify(notificacionService).registrar(DUENO, TipoEvento.NUEVA_ORDEN,
                "Se registró la orden LG-0001 del paciente Código 1000.", ORDEN);
    }

    /** RN-11: el aviso al laboratorio depende de tipo_orden.notifica_admin, no del código del tipo. */
    @Test
    void rn11UnaOrdenNormalNoAvisaAlLaboratorio() {
        listener.alRegistrarseUnaOrden(ordenCreada(false));

        verify(notificacionService, never()).registrar(anyLong(), eq(TipoEvento.ORDEN_URGENTE), anyString(), anyLong());
        verify(usuarioService, never()).listarAdministradoresActivosParaNotificacion();
    }

    /** §6.2: "Administrador" no es una cuenta sino un rol; le llega a cada cuenta activa. */
    @Test
    void rn11UnaOrdenUrgenteAvisaACadaAdministradorActivo() {
        hayDosAdministradoresActivos();

        listener.alRegistrarseUnaOrden(ordenCreada(true));

        String urgente = "Se registró la orden urgente LG-0001 del paciente Código 1000.";
        verify(notificacionService).registrar(ADMIN, TipoEvento.ORDEN_URGENTE, urgente, ORDEN);
        verify(notificacionService).registrar(SUPERADMIN, TipoEvento.ORDEN_URGENTE, urgente, ORDEN);
    }

    /** Una orden urgente genera las dos cosas: el aviso al dueño y el aviso al laboratorio. */
    @Test
    void unaOrdenUrgenteNoReemplazaElAvisoAlDueno() {
        hayDosAdministradoresActivos();

        listener.alRegistrarseUnaOrden(ordenCreada(true));

        verify(notificacionService).registrar(eq(DUENO), eq(TipoEvento.NUEVA_ORDEN), anyString(), eq(ORDEN));
    }

    /** RN-05/CU-07: el texto es el documentado, y el destinatario es el dueño de la orden. */
    @Test
    void cu07ElCambioDeEstadoAvisaAlDuenoConElTextoDocumentado() {
        listener.alCambiarDeEstado(
                new EstadoOrdenCambiadoEvent(ORDEN, "LG-0001", DUENO, PACIENTE, "En producción"));

        verify(notificacionService).registrar(DUENO, TipoEvento.CAMBIO_ESTADO,
                "El trabajo del paciente Código 1000 pasó a la etapa de En producción.", ORDEN);
    }

    /**
     * §5.5 criterio 4 ("si la transacción falla, no se envía notificación") y Agente.md 5.6.
     * Ese criterio no depende de ninguna lógica de este listener sino de *cuándo* se lo invoca:
     * si alguien sacara el AFTER_COMMIT, un rollback dejaría notificaciones de órdenes que no
     * existen y ningún test de comportamiento lo detectaría. Por eso se fija la anotación.
     */
    @Test
    void criterio4LosDosListenersCorrenReciénDespuésDelCommit() {
        List<Method> escuchas = Arrays.stream(OrdenNotificacionListener.class.getDeclaredMethods())
                .filter(metodo -> metodo.isAnnotationPresent(TransactionalEventListener.class))
                .toList();

        assertThat(escuchas).hasSize(2);
        assertThat(escuchas).allSatisfy(metodo -> assertThat(
                metodo.getAnnotation(TransactionalEventListener.class).phase())
                .isEqualTo(TransactionPhase.AFTER_COMMIT));
    }

    /**
     * S-08 sin resolver: la cancelación no notifica. OrdenEstadoService.cancelar no publica
     * evento y acá no hay quien lo escuche; si alguien agrega un tercer listener, este test avisa.
     */
    @Test
    void s08NoHayNingunListenerDeMasEscuchandoOtrosEventos() {
        List<Class<?>> eventosEscuchados = Arrays.stream(OrdenNotificacionListener.class.getDeclaredMethods())
                .filter(metodo -> metodo.isAnnotationPresent(TransactionalEventListener.class))
                .<Class<?>>map(metodo -> metodo.getParameterTypes()[0])
                .toList();

        assertThat(eventosEscuchados)
                .containsExactlyInAnyOrder(OrdenCreadaEvent.class, EstadoOrdenCambiadoEvent.class);
    }
}
