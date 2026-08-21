package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.TipoEvento;
import com.labgarcias.seguridad.domain.CredencialesCreadasEvent;

@ExtendWith(MockitoExtension.class)
class CredencialesNotificacionListenerTest {

    private static final long USUARIO = 7L;
    private static final String PASSWORD = "Ab3$Kd9!Xz2P";

    private final CredencialesCreadasEvent evento =
            new CredencialesCreadasEvent(USUARIO, "juan@mail.com", "jperez", PASSWORD);

    @Mock
    private NotificacionService notificacionService;
    @Mock
    private CanalCorreo canalCorreo;

    @InjectMocks
    private CredencialesNotificacionListener listener;

    private String cuerpoEnviado() {
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(canalCorreo).enviarCorreo(eq("juan@mail.com"), anyString(), cuerpo.capture());
        return cuerpo.getValue();
    }

    /** §3.1.b: el correo lleva el nombre de usuario, la contraseña temporal y el aviso del cambio. */
    @Test
    void elCorreoLlevaLasCredencialesYElAvisoDeCambio() {
        listener.alCrearseLasCredenciales(evento);

        assertThat(cuerpoEnviado())
                .contains("jperez")
                .contains(PASSWORD)
                .containsIgnoringCase("cambies");
    }

    /** §3.1.b criterio 3: el envío queda registrado en el outbox, ya resuelto como ENVIADO. */
    @Test
    void criterio3RegistraElEnvioComoEnviado() {
        listener.alCrearseLasCredenciales(evento);

        verify(notificacionService).registrarConEnvioResuelto(
                eq(USUARIO), eq(TipoEvento.CREDENCIALES_CREADAS), anyString(), eq(Canal.CORREO), isNull());
    }

    /**
     * §3.1.b: lo que se persiste es el texto genérico. Es el punto donde se decide que la
     * contraseña no quede en claro en la base.
     */
    @Test
    void elMensajeDelOutboxNoContieneLaContrasena() {
        listener.alCrearseLasCredenciales(evento);

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).registrarConEnvioResuelto(
                eq(USUARIO), eq(TipoEvento.CREDENCIALES_CREADAS), mensaje.capture(), eq(Canal.CORREO), isNull());

        assertThat(mensaje.getValue())
                .doesNotContain(PASSWORD)
                .doesNotContain("jperez")
                .isEqualTo("Se creó tu cuenta en Lab. Garcia's Connect. Revisá tu correo por las credenciales de acceso.");
    }

    /** §6 criterio 2: si el SMTP falla, queda FALLIDO con su motivo y la cuenta ya está creada igual. */
    @Test
    void siElCorreoFallaElEnvioQuedaFallidoConSuMotivo() {
        doThrow(new EnvioNoRealizadoException("Connection refused"))
                .when(canalCorreo).enviarCorreo(anyString(), anyString(), anyString());

        listener.alCrearseLasCredenciales(evento);

        verify(notificacionService).registrarConEnvioResuelto(
                eq(USUARIO), eq(TipoEvento.CREDENCIALES_CREADAS), anyString(), eq(Canal.CORREO),
                eq("Connection refused"));
    }

    /** Agente.md 5.6: si el alta se deshace, no queda ni notificación ni correo enviado. */
    @Test
    void elListenerCorreDespuesDelCommit() {
        Method metodo = Arrays.stream(CredencialesNotificacionListener.class.getDeclaredMethods())
                .filter(candidato -> candidato.getName().equals("alCrearseLasCredenciales"))
                .findFirst()
                .orElseThrow();

        TransactionalEventListener anotacion = metodo.getAnnotation(TransactionalEventListener.class);
        assertThat(anotacion).isNotNull();
        assertThat(anotacion.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    /**
     * §3.1.b: la contraseña no puede aparecer en un log. `toString` es la vía más probable de
     * fuga, porque cualquier log del evento la imprimiría.
     */
    @Test
    void elEventoNoRevelaLaContrasenaAlImprimirse() {
        assertThat(evento.toString())
                .doesNotContain(PASSWORD)
                .contains("****")
                .contains("jperez");
    }
}
