package br.ufrn.imd.imdtravel.obs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class OutboundCallInterceptorTest {

    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final JsonLogger logger = new JsonLogger(new PrintStream(bytes, true, StandardCharsets.UTF_8));

    @AfterEach
    void limpa() {
        CorrelationContext.clear();
    }

    @Test
    void propagaOCidERegistraChamadaConcluida() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://fidelity")
                .requestInterceptor(new OutboundCallInterceptor(logger, "fidelity"));
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://fidelity/points"))
                .andExpect(header("X-Correlation-Id", "cid-prop"))
                .andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));

        CorrelationContext.set("cid-prop");
        builder.build().get().uri("/points").retrieve().body(String.class);

        server.verify();
        JsonNode j = new ObjectMapper().readTree(bytes.toString(StandardCharsets.UTF_8).trim());
        assertThat(j.get("event").asText()).isEqualTo("chamada.concluida");
        assertThat(j.get("dest").asText()).isEqualTo("fidelity");
        assertThat(j.get("method").asText()).isEqualTo("GET");
        assertThat(j.get("path").asText()).isEqualTo("/points");
        assertThat(j.get("status").asInt()).isEqualTo(200);
        assertThat(j.get("cid").asText()).isEqualTo("cid-prop");
    }

    @Test
    void falhaDeConexaoViraChamadaFalhouERelancaAMesmaExcecao() throws Exception {
        OutboundCallInterceptor interceptor = new OutboundCallInterceptor(logger, "exchange");
        IOException original = new IOException("conexão recusada");
        org.springframework.http.client.ClientHttpRequestExecution falha = (req, body) -> {
            throw original;
        };
        org.springframework.mock.http.client.MockClientHttpRequest req =
                new org.springframework.mock.http.client.MockClientHttpRequest(
                        org.springframework.http.HttpMethod.GET, java.net.URI.create("http://exchange/convert"));

        CorrelationContext.set("cid-f");
        assertThatThrownBy(() -> interceptor.intercept(req, new byte[0], falha)).isSameAs(original);

        JsonNode j = new ObjectMapper().readTree(bytes.toString(StandardCharsets.UTF_8).trim());
        assertThat(j.get("event").asText()).isEqualTo("chamada.falhou");
        assertThat(j.get("dest").asText()).isEqualTo("exchange");
        assertThat(j.get("path").asText()).isEqualTo("/convert");
        assertThat(j.get("erro").asText()).contains("IOException").contains("conexão recusada");
        assertThat(req.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("cid-f");
    }
}
