package com.labgarcias.shared.excepcion;

import org.springframework.http.HttpStatus;

/** 400: dato de entrada inválido con un código específico (ej.: RN-15, PASSWORD_INVALIDA). */
public class ValidacionException extends DominioException {

    public ValidacionException(String codigo, String mensaje, String campo) {
        super(codigo, mensaje, campo);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
