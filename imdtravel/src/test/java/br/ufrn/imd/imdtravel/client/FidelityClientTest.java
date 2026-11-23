package br.ufrn.imd.imdtravel.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.ufrn.imd.imdtravel.client.dto.BonusResult;
import br.ufrn.imd.imdtravel.service.exception.SoldOutException;

class FidelityClientTest {

    private MockRestServiceServer server;
    private FidelityClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://fidelity");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FidelityClient(builder.build());
    }

    @Test
    void addBonusCaminhoFeliz() {
        server.expect(requestTo("http://fidelity/bonus"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user").value("ana"))
                .andExpect(jsonPath("$.bonus").value(350))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"user\":\"ana\",\"totalPoints\":350}"));

        BonusResult result = client.addBonus("ana", 350);

        assertThat(result.user()).isEqualTo("ana");
        assertThat(result.totalPoints()).isEqualTo(350);
        server.verify();
    }

    @Test
    void addBonus500PropagaRestClientException() {
        server.expect(requestTo("http://fidelity/bonus"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.addBonus("ana", 350))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    void addBonus409NaoETraduzido() {
        server.expect(requestTo("http://fidelity/bonus"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.addBonus("ana", 350))
                .isInstanceOf(RestClientException.class)
                .isNotInstanceOf(SoldOutException.class);
    }
}