package com.labgarcias.shared.excepcion;

import org.springframework.http.HttpStatus;

/** 409: conflicto de estado o de unicidad (ej.: correo duplicado, transición no permitida). */
public class ConflictoException extends DominioException {

    public ConflictoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }

    public ConflictoException(String codigo, String mensaje, String campo) {
        super(codigo, mensaje, campo);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
