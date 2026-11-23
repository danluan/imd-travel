package br.ufrn.imd.imdtravel.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import br.ufrn.imd.imdtravel.client.dto.FlightInfo;
import br.ufrn.imd.imdtravel.client.dto.SellRequest;
import br.ufrn.imd.imdtravel.client.dto.SellResult;
import br.ufrn.imd.imdtravel.service.exception.FlightNotFoundException;
import br.ufrn.imd.imdtravel.service.exception.SoldOutException;

@Component
public class AirlinesHubClient {

    private final RestClient restClient;

    public AirlinesHubClient(@Qualifier("airlinesHubRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** GET /flight?flight=&day= ; 404 vira FlightNotFoundException. */
    public FlightInfo getFlight(String flight, String day) {
        return restClient.get()
                .uri(uri -> uri.path("/flight")
                        .queryParam("flight", flight)
                        .queryParam("day", day)
                        .build())
                .retrieve()
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    throw new FlightNotFoundException(flight, day);
                })
                .body(FlightInfo.class);
    }

    /** POST /sell ; 409 vira SoldOutException. */
    public SellResult sell(String flight, String day) {
        return restClient.post()
                .uri("/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SellRequest(flight, day))
                .retrieve()
                .onStatus(status -> status.value() == 409, (req, res) -> {
                    throw new SoldOutException(flight, day);
                })
                .body(SellResult.class);
    }
}