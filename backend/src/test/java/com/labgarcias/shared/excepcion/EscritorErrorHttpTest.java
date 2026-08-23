package com.labgarcias.shared.excepcion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class EscritorErrorHttpTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * El motivo de que esta clase exista: sin fijar el charset, `getWriter()` usa ISO-8859-1 y
     * cualquier mensaje del dominio —todos en español— llega ilegible.
     */
    @Test
    void escribeEnUtf8ParaQueLosAcentosLleguenIntactos() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EscritorErrorHttp.escribir(response, objectMapper, 403, "SIN_PERMISO",
                "No tenés permiso para esta operación.");

        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("No tenés permiso para esta operación.");
    }

    /** Mismo contrato que devuelve ManejadorGlobalExcepciones: codigo, mensaje y campo. */
    @Test
    void usaElMismoCuerpoQueElManejadorGlobal() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EscritorErrorHttp.escribir(response, objectMapper, 423, "LICENCIA_VENCIDA", "Licencia vencida.");

        JsonNode cuerpo = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(response.getStatus()).isEqualTo(423);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(cuerpo.get("codigo").asText()).isEqualTo("LICENCIA_VENCIDA");
        assertThat(cuerpo.get("mensaje").asText()).isEqualTo("Licencia vencida.");
        assertThat(cuerpo.get("campo").isNull()).isTrue();
    }
}
