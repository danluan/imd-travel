package br.ufrn.imd.imdtravel.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.ufrn.imd.imdtravel.client.dto.ExchangeRate;

class ExchangeClientTest {

    private MockRestServiceServer server;
    private ExchangeClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://exchange");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ExchangeClient(builder.build());
    }

    @Test
    void getRateCaminhoFeliz() {
        server.expect(requestTo("http://exchange/convert"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"rate\":5.4784,\"asOf\":\"2026-11-23T20:00:00Z\"}",
                        MediaType.APPLICATION_JSON));

        ExchangeRate rate = client.getRate();

        assertThat(rate.rate()).isEqualByComparingTo(new BigDecimal("5.4784"));
        assertThat(rate.asOf()).isEqualTo("2026-11-23T20:00:00Z");
        server.verify();
    }

    @Test
    void getRate500PropagaRestClientException() {
        server.expect(requestTo("http://exchange/convert"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getRate())
                .isInstanceOf(RestClientException.class);
    }

    @Test
    void getRate404NaoETraduzido() {
        server.expect(requestTo("http://exchange/convert"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getRate())
                .isInstanceOf(RestClientException.class)
                .hasMessageContaining("404");
    }
}