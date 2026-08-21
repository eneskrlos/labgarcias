package com.labgarcias.shared.excepcion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Escribe un `ErrorRespuesta` directo sobre la respuesta HTTP, con el mismo contrato que devuelve
 * `ManejadorGlobalExcepciones`.
 *
 * Existe porque los errores que se responden **fuera** de un `@RestControllerAdvice` —los filtros
 * y los handlers de Spring Security— se arman a mano, y ahí es fácil olvidarse del charset: sin
 * él, `getWriter()` usa ISO-8859-1 y todo mensaje con acentos llega ilegible al cliente. Pasó en
 * dos de los tres lugares antes de centralizarlo acá.
 */
public final class EscritorErrorHttp {

    private EscritorErrorHttp() {
    }

    public static void escribir(HttpServletResponse response, ObjectMapper objectMapper,
                                int status, String codigo, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorRespuesta(codigo, mensaje, null)));
    }
}
