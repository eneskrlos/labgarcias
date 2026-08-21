package com.labgarcias.seguridad.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * §3.1.b paso 2: la contraseña temporal del alta por el administrador.
 *
 * Se arma tomando un carácter obligatorio de cada categoría de RN-15 y completando con el
 * alfabeto entero, para que cumplir la regla no dependa de la suerte del sorteo. Después se
 * mezcla, así las posiciones fijas no filtran la composición.
 *
 * El valor que devuelve **no se persiste ni se loguea en ningún punto**: viaja en memoria hasta
 * el correo, y a la base va solo su hash BCrypt.
 */
@Component
public class GeneradorPassword {

    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITOS = "23456789";
    private static final String ESPECIALES = "!@#$%&*?+-";

    /**
     * Por encima del mínimo de 9 de RN-15: la contraseña viaja por correo y vive hasta el primer
     * ingreso, así que conviene que sobre margen. RN-15 fija un piso, no una longitud exacta.
     */
    private static final int LONGITUD = 12;

    private final SecureRandom aleatorio = new SecureRandom();

    public String generar() {
        List<Character> caracteres = new ArrayList<>(LONGITUD);
        caracteres.add(unoDe(MAYUSCULAS));
        caracteres.add(unoDe(MINUSCULAS));
        caracteres.add(unoDe(DIGITOS));
        caracteres.add(unoDe(ESPECIALES));

        String alfabeto = MAYUSCULAS + MINUSCULAS + DIGITOS + ESPECIALES;
        while (caracteres.size() < LONGITUD) {
            caracteres.add(unoDe(alfabeto));
        }

        return mezclado(caracteres);
    }

    private char unoDe(String alfabeto) {
        return alfabeto.charAt(aleatorio.nextInt(alfabeto.length()));
    }

    /** Fisher-Yates con SecureRandom: Collections.shuffle usaría un Random previsible. */
    private String mezclado(List<Character> caracteres) {
        StringBuilder resultado = new StringBuilder(caracteres.size());
        for (int i = caracteres.size() - 1; i > 0; i--) {
            int j = aleatorio.nextInt(i + 1);
            Character intercambiado = caracteres.get(i);
            caracteres.set(i, caracteres.get(j));
            caracteres.set(j, intercambiado);
        }
        caracteres.forEach(resultado::append);
        return resultado.toString();
    }
}
