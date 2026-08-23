package com.labgarcias.seguridad.service;

import com.labgarcias.shared.excepcion.ValidacionException;
import com.labgarcias.shared.util.ConstantesDominio;

/**
 * RN-15: la misma exigencia para la contraseña que genera el sistema (§3.1.b paso 2) y para la
 * que elige el usuario en el cambio obligatorio. Un solo lugar, para que no se separen.
 */
public final class ValidadorPassword {

    private static final String MENSAJE = "La contraseña debe tener al menos "
            + ConstantesDominio.LONGITUD_MINIMA_PASSWORD
            + " caracteres, con mayúsculas, minúsculas, números y caracteres especiales.";

    private ValidadorPassword() {
    }

    public static void validar(String password, String campo) {
        if (!cumpleRn15(password)) {
            throw new ValidacionException("PASSWORD_INVALIDA", MENSAJE, campo);
        }
    }

    public static boolean cumpleRn15(String password) {
        return password != null
                && password.length() >= ConstantesDominio.LONGITUD_MINIMA_PASSWORD
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit)
                && password.chars().anyMatch(ValidadorPassword::esEspecial);
    }

    /** Especial es todo lo que no sea letra ni dígito: no hay una lista documentada. */
    private static boolean esEspecial(int caracter) {
        return !Character.isLetterOrDigit(caracter) && !Character.isWhitespace(caracter);
    }
}
