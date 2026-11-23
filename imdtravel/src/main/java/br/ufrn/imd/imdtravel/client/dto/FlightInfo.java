package br.ufrn.imd.imdtravel.client.dto;

import java.math.BigDecimal;

public record FlightInfo(
        String flight,
        String day,
        BigDecimal priceUSD,
        int seatsAvailable
) {}