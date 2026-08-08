package com.labgarcias.shared.excepcion;

import org.springframework.http.HttpStatus;

/** 404: el recurso solicitado no existe o no pertenece al usuario autenticado (RN-01). */
public class RecursoNoEncontradoException extends DominioException {

    public RecursoNoEncontradoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
