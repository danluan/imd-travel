package br.ufrn.imd.imdtravel.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
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

import br.ufrn.imd.imdtravel.client.dto.FlightInfo;
import br.ufrn.imd.imdtravel.client.dto.SellResult;
import br.ufrn.imd.imdtravel.service.exception.FlightNotFoundException;
import br.ufrn.imd.imdtravel.service.exception.SoldOutException;

class AirlinesHubClientTest {

    private MockRestServiceServer server;
    private AirlinesHubClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://airlineshub");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AirlinesHubClient(builder.build());
    }

    @Test
    void getFlightCaminhoFeliz() {
        server.expect(requestTo("http://airlineshub/flight?flight=IMD100&day=2026-12-01"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"flight\":\"IMD100\",\"day\":\"2026-12-01\",\"priceUSD\":350.50,\"seatsAvailable\":20}",
                        MediaType.APPLICATION_JSON));

        FlightInfo info = client.getFlight("IMD100", "2026-12-01");

        assertThat(info.flight()).isEqualTo("IMD100");
        assertThat(info.priceUSD()).isEqualByComparingTo(new BigDecimal("350.50"));
        assertThat(info.seatsAvailable()).isEqualTo(20);
        server.verify();
    }

    @Test
    void getFlight404ViraFlightNotFoundException() {
        server.expect(requestTo("http://airlineshub/flight?flight=XX&day=2026-12-01"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getFlight("XX", "2026-12-01"))
                .isInstanceOf(FlightNotFoundException.class);
    }

    @Test
    void getFlight500PropagaRestClientException() {
        server.expect(requestTo("http://airlineshub/flight?flight=IMD100&day=2026-12-01"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getFlight("IMD100", "2026-12-01"))
                .isInstanceOf(RestClientException.class)
                .isNotInstanceOf(FlightNotFoundException.class);
    }

    @Test
    void sellCaminhoFeliz() {
        server.expect(requestTo("http://airlineshub/sell"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flight").value("IMD100"))
                .andExpect(jsonPath("$.day").value("2026-12-01"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"transactionId\":\"tx-1\",\"seatsRemaining\":19}"));

        SellResult result = client.sell("IMD100", "2026-12-01");

        assertThat(result.transactionId()).isEqualTo("tx-1");
        assertThat(result.seatsRemaining()).isEqualTo(19);
        server.verify();
    }

    @Test
    void sell409ViraSoldOutException() {
        server.expect(requestTo("http://airlineshub/sell"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"sold-out\"}"));

        assertThatThrownBy(() -> client.sell("IMD100", "2026-12-01"))
                .isInstanceOf(SoldOutException.class);
    }

    @Test
    void sell500PropagaRestClientException() {
        server.expect(requestTo("http://airlineshub/sell"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.sell("IMD100", "2026-12-01"))
                .isInstanceOf(RestClientException.class)
                .isNotInstanceOf(SoldOutException.class);
    }
}