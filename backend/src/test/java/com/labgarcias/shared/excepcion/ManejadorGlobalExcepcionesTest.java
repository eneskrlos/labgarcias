package com.labgarcias.shared.excepcion;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
class ManejadorGlobalExcepcionesTest {

    private final ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones();

    @Mock
    private MethodParameter methodParameter;

    @Mock
    private BindingResult bindingResult;

    /**
     * ResponseEntity.getBody() es @Nullable, así que encadenar sobre él daría un NPE opaco si el
     * manejador dejara de poblar el cuerpo. Se materializa una sola vez y se falla con un mensaje
     * que dice qué se rompió.
     */
    private static ErrorRespuesta cuerpoDe(ResponseEntity<ErrorRespuesta> respuesta) {
        return requireNonNull(respuesta.getBody(), "el manejador debe devolver siempre un cuerpo de error");
    }

    @Test
    void manejarDominioUsaElHttpStatusDeLaExcepcion() {
        DominioException excepcion = new ReglaNegocioException("PRECIO_INSUFICIENTE", "El precio es menor al mínimo.", "precio");

        ResponseEntity<ErrorRespuesta> respuesta = manejador.manejarDominio(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(cuerpoDe(respuesta).codigo()).isEqualTo("PRECIO_INSUFICIENTE");
        assertThat(cuerpoDe(respuesta).mensaje()).isEqualTo("El precio es menor al mínimo.");
        assertThat(cuerpoDe(respuesta).campo()).isEqualTo("precio");
    }

    @Test
    void manejarValidacionUsaElPrimerFieldError() {
        FieldError error = new FieldError("objeto", "correo", "El correo es obligatorio.");
        when(bindingResult.getFieldError()).thenReturn(error);
        MethodArgumentNotValidException excepcion = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorRespuesta> respuesta = manejador.manejarValidacion(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cuerpoDe(respuesta).codigo()).isEqualTo("VALIDACION");
        assertThat(cuerpoDe(respuesta).campo()).isEqualTo("correo");
        assertThat(cuerpoDe(respuesta).mensaje()).isEqualTo("El correo es obligatorio.");
    }

    @Test
    void manejarValidacionSinFieldErrorUsaMensajeGenerico() {
        when(bindingResult.getFieldError()).thenReturn(null);
        MethodArgumentNotValidException excepcion = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorRespuesta> respuesta = manejador.manejarValidacion(excepcion);

        assertThat(cuerpoDe(respuesta).campo()).isNull();
        assertThat(cuerpoDe(respuesta).mensaje()).isEqualTo("Datos inválidos.");
    }

    @Test
    void manejarViolacionRestriccionDevuelve400() {
        ConstraintViolationException excepcion = new ConstraintViolationException("tamaño inválido", Collections.emptySet());

        ResponseEntity<ErrorRespuesta> respuesta = manejador.manejarViolacionRestriccion(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cuerpoDe(respuesta).codigo()).isEqualTo("VALIDACION");
    }

    @Test
    void manejarParametroFaltanteExponeElNombreDelParametroComoCampo() {
        MissingServletRequestParameterException excepcion = new MissingServletRequestParameterException("token", "String");

        ResponseEntity<ErrorRespuesta> respuesta = manejador.manejarParametroFaltante(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cuerpoDe(respuesta).campo()).isEqualTo("token");
    }

    @Test
    void manejarAutorizacionDenegadaDevuelve403NoQuinientos() {
        AuthorizationDeniedException excepcion = new AuthorizationDeniedException("Access Denied");

        ResponseEntity<ErrorRespuesta> respuesta = manejador.manejarAutorizacionDenegada(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(cuerpoDe(respuesta).codigo()).isEqualTo("SIN_PERMISO");
    }

    @Test
    void manejarRecursoNoEncontradoDevuelve404NoQuinientos() {
        NoResourceFoundException excepcion = new NoResourceFoundException(HttpMethod.GET, "swagger-ui/index.html");

        ResponseEntity<ErrorRespuesta> respuesta = manejador.manejarRecursoNoEncontrado(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(cuerpoDe(respuesta).codigo()).isEqualTo("NO_ENCONTRADO");
    }

    @Test
    void manejarErrorGenericoDevuelve500SinFiltrarElMensajeOriginal() {
        ResponseEntity<ErrorRespuesta> respuesta = manejador.manejarError(new IllegalStateException("detalle interno sensible"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(cuerpoDe(respuesta).codigo()).isEqualTo("ERROR_INTERNO");
        assertThat(cuerpoDe(respuesta).mensaje()).doesNotContain("detalle interno sensible");
    }
}
