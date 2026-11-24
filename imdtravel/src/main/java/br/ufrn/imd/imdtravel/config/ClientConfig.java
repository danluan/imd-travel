package br.ufrn.imd.imdtravel.config;

import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import br.ufrn.imd.imdtravel.obs.JsonLogger;
import br.ufrn.imd.imdtravel.obs.OutboundCallInterceptor;

/**
 * Beans RestClient das três dependências.
 *
 * ATENÇÃO (D-08 / R-16): de propósito NÃO há timeout de conexão nem de leitura.
 * Vale o padrão do JDK HttpClient (espera indefinida). Não "melhorar" isso:
 * a Parte 1 existe para observar esse comportamento.
 */
@Configuration
public class ClientConfig {

    private RestClient build(String baseUrl, JsonLogger logger, String dest) {
        // HTTP/1.1 explícito evita o upgrade h2c do JDK, que alguns servidores
        // não toleram em POST. Isso não adiciona timeout nenhum.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        // Sem setReadTimeout, de propósito.
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                // Propaga o X-Correlation-Id (R-12) e registra a chamada; não muda o comportamento.
                .requestInterceptor(new OutboundCallInterceptor(logger, dest))
                .build();
    }

    @Bean
    RestClient airlinesHubRestClient(@Value("${imdtravel.airlineshub-url}") String url, JsonLogger logger) {
        return build(url, logger, "airlineshub");
    }

    @Bean
    RestClient exchangeRestClient(@Value("${imdtravel.exchange-url}") String url, JsonLogger logger) {
        return build(url, logger, "exchange");
    }

    @Bean
    RestClient fidelityRestClient(@Value("${imdtravel.fidelity-url}") String url, JsonLogger logger) {
        return build(url, logger, "fidelity");
    }
}