package br.ufrn.imd.imdtravel.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import br.ufrn.imd.imdtravel.client.dto.BonusRequest;
import br.ufrn.imd.imdtravel.client.dto.BonusResult;

@Component
public class FidelityClient {

    private final RestClient restClient;

    public FidelityClient(@Qualifier("fidelityRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** POST /bonus. Nenhum erro é traduzido: qualquer falha propaga. */
    public BonusResult addBonus(String user, int bonus) {
        return restClient.post()
                .uri("/bonus")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BonusRequest(user, bonus))
                .retrieve()
                .body(BonusResult.class);
    }
}