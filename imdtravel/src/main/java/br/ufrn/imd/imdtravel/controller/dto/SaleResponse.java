package br.ufrn.imd.imdtravel.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;

import br.ufrn.imd.imdtravel.domain.Sale;

public record SaleResponse(String correlationId, String transactionId, String flight, String day,
                           String user, BigDecimal valueUSD, BigDecimal valueBRL, Instant ts) {

    public static SaleResponse from(Sale s) {
        return new SaleResponse(s.getCorrelationId(), s.getTransactionId(), s.getFlight(),
                s.getDay(), s.getUser(), s.getValueUSD(), s.getValueBRL(), s.getTs());
    }
}
