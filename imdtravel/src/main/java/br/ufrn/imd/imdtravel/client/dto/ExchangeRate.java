package br.ufrn.imd.imdtravel.client.dto;

import java.math.BigDecimal;

public record ExchangeRate(BigDecimal rate, String asOf) {}