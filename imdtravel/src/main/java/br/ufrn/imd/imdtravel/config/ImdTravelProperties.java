package br.ufrn.imd.imdtravel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "imdtravel")
public record ImdTravelProperties(String airlineshubUrl, String exchangeUrl, String fidelityUrl, String salesDir) {
}
