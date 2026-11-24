package br.ufrn.imd.imdtravel.obs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonLoggerTest {

    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final JsonLogger logger = new JsonLogger(new PrintStream(bytes, true, StandardCharsets.UTF_8));

    @AfterEach
    void limpa() {
        CorrelationContext.clear();
    }

    private String[] linhas() {
        return bytes.toString(StandardCharsets.UTF_8).split("\n");
    }

    @Test
    void produzUmaLinhaJsonComOsCincoCampos() throws Exception {
        CorrelationContext.set("cid-x");
        logger.log("teste.evento", 42, Map.of("path", "/x"));

        assertThat(linhas()).hasSize(1);
        JsonNode j = new ObjectMapper().readTree(linhas()[0]);
        assertThat(j.get("svc").asText()).isEqualTo("imdtravel");
        assertThat(j.get("cid").asText()).isEqualTo("cid-x");
        assertThat(j.get("event").asText()).isEqualTo("teste.evento");
        assertThat(j.get("ms").isNumber()).isTrue();
        assertThat(j.get("ms").asLong()).isEqualTo(42);
        assertThat(j.get("ts").asText()).endsWith("Z");
        assertThat(j.get("path").asText()).isEqualTo("/x");
    }

    @Test
    void foraDeRequisicaoOCidENuloEMsEZero() throws Exception {
        logger.log("app.iniciada");

        JsonNode j = new ObjectMapper().readTree(linhas()[0]);
        assertThat(j.has("cid")).isTrue();
        assertThat(j.get("cid").isNull()).isTrue();
        assertThat(j.get("ms").asLong()).isZero();
    }

    @Test
    void mensagemComQuebraDeLinhaContinuaEmUmaLinha() throws Exception {
        logger.log("erro.interno", 0, Map.of("erro", "a\nb"));

        assertThat(linhas()).hasSize(1);
        assertThat(new ObjectMapper().readTree(linhas()[0]).get("erro").asText()).isEqualTo("a\nb");
    }
}
