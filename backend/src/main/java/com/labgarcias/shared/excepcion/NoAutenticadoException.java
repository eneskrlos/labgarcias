package com.labgarcias.shared.excepcion;

import org.springframework.http.HttpStatus;

/** 401: credenciales inválidas o solicitud sin autenticar. */
public class NoAutenticadoException extends DominioException {

    public NoAutenticadoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
