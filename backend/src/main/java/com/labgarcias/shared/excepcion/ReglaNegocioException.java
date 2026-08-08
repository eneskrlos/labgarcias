package com.labgarcias.shared.excepcion;

import org.springframework.http.HttpStatus;

/** 422: regla de negocio incumplida (ej.: precio insuficiente, tipo de trabajo inactivo). */
public class ReglaNegocioException extends DominioException {

    public ReglaNegocioException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }

    public ReglaNegocioException(String codigo, String mensaje, String campo) {
        super(codigo, mensaje, campo);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
