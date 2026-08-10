package com.labgarcias.shared.excepcion;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);
    private static final String CODIGO_VALIDACION = "VALIDACION";
    private static final String CODIGO_NO_ENCONTRADO = "NO_ENCONTRADO";
    private static final String CODIGO_SIN_PERMISO = "SIN_PERMISO";
    private static final String CODIGO_ERROR_INTERNO = "ERROR_INTERNO";

    @ExceptionHandler(DominioException.class)
    public ResponseEntity<ErrorRespuesta> manejarDominio(DominioException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(new ErrorRespuesta(ex.getCodigo(), ex.getMessage(), ex.getCampo()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException ex) {
        FieldError error = ex.getBindingResult().getFieldError();
        String campo = error != null ? error.getField() : null;
        String mensaje = error != null ? error.getDefaultMessage() : "Datos inválidos.";
        return ResponseEntity.badRequest().body(new ErrorRespuesta(CODIGO_VALIDACION, mensaje, campo));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorRespuesta> manejarViolacionRestriccion(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(new ErrorRespuesta(CODIGO_VALIDACION, ex.getMessage(), null));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> manejarParametroFaltante(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorRespuesta(CODIGO_VALIDACION, ex.getMessage(), ex.getParameterName()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorRespuesta> manejarAutorizacionDenegada(AuthorizationDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorRespuesta(CODIGO_SIN_PERMISO, "No tenés permiso para esta operación.", null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> manejarRecursoNoEncontrado(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorRespuesta(CODIGO_NO_ENCONTRADO, "Recurso no encontrado.", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> manejarError(Exception ex) {
        log.error("Error no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorRespuesta(CODIGO_ERROR_INTERNO, "Ocurrió un error inesperado.", null));
    }
}
