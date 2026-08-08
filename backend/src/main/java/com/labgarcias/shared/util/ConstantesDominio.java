package com.labgarcias.shared.util;

import java.math.BigDecimal;
import java.util.List;

public final class ConstantesDominio {

    private ConstantesDominio() {
    }

    /** RN-12: mínimo de días hábiles de confección de un tipo de trabajo. */
    public static final int DIAS_HABILES_MINIMOS = 7;

    /** RN-21: precio mínimo de un tipo de trabajo. */
    public static final BigDecimal PRECIO_MINIMO_TRABAJO = new BigDecimal("250");

    /** RN-15: longitud mínima de la contraseña. */
    public static final int LONGITUD_MINIMA_PASSWORD = 9;

    /** RN-13: tamaño máximo de una imagen adjunta, en bytes (5 MB). */
    public static final long TAMANO_MAXIMO_IMAGEN = 5_242_880L;

    /** RN-13: tamaño máximo de un documento adjunto, en bytes (8 MB). */
    public static final long TAMANO_MAXIMO_DOCUMENTO = 8_388_608L;

    /** D-02: horas de vigencia del token de verificación de cuenta. */
    public static final int HORAS_VIGENCIA_TOKEN = 24;

    /** RN-13: formatos de imagen permitidos como adjunto. */
    public static final List<String> FORMATOS_IMAGEN = List.of("image/jpeg", "image/png");

    /** RN-13: formatos de documento permitidos como adjunto. */
    public static final List<String> FORMATOS_DOCUMENTO = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
}
