package com.labgarcias.shared.excepcion;

import org.springframework.http.HttpStatus;

/** 403: usuario identificado pero sin permiso para la operación (ej.: cuenta sin verificar). */
public class AccesoDenegadoException extends DominioException {

    public AccesoDenegadoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
