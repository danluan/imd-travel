package br.ufrn.imd.imdtravel.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import br.ufrn.imd.imdtravel.client.dto.ExchangeRate;

@Component
public class ExchangeClient {

    private final RestClient restClient;

    public ExchangeClient(@Qualifier("exchangeRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** GET /convert. Nenhum erro é traduzido: qualquer falha propaga. */
    public ExchangeRate getRate() {
        return restClient.get()
                .uri("/convert")
                .retrieve()
                .body(ExchangeRate.class);
    }
}