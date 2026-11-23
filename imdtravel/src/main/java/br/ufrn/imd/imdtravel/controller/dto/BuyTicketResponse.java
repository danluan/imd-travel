package br.ufrn.imd.imdtravel.controller.dto;

import java.math.BigDecimal;

public record BuyTicketResponse(
        String transactionId,
        String flight,
        String day,
        String user,
        BigDecimal valueUSD,
        BigDecimal valueBRL,
        BigDecimal rate,
        String rateAsOf,
        int bonus
) {}