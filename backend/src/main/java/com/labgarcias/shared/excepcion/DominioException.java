package com.labgarcias.shared.excepcion;

import org.springframework.http.HttpStatus;

/**
 * Base de las excepciones propias del dominio. Cada subclase fija el HttpStatus
 * de su familia de error; el {@code codigo} identifica la regla incumplida
 * (spec.md §1: cuerpo uniforme de error).
 */
public abstract class DominioException extends RuntimeException {

    private final String codigo;
    private final String campo;

    protected DominioException(String codigo, String mensaje) {
        this(codigo, mensaje, null);
    }

    protected DominioException(String codigo, String mensaje, String campo) {
        super(mensaje);
        this.codigo = codigo;
        this.campo = campo;
    }

    public abstract HttpStatus getHttpStatus();

    public String getCodigo() {
        return codigo;
    }

    public String getCampo() {
        return campo;
    }
}
