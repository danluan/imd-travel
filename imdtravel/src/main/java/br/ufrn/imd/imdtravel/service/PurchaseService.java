package br.ufrn.imd.imdtravel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import org.springframework.stereotype.Service;

import br.ufrn.imd.imdtravel.client.AirlinesHubClient;
import br.ufrn.imd.imdtravel.client.ExchangeClient;
import br.ufrn.imd.imdtravel.client.FidelityClient;
import br.ufrn.imd.imdtravel.client.dto.ExchangeRate;
import br.ufrn.imd.imdtravel.client.dto.FlightInfo;
import br.ufrn.imd.imdtravel.client.dto.SellResult;
import br.ufrn.imd.imdtravel.controller.dto.BuyTicketResponse;

/**
 * Orquestra a compra: 4 chamadas síncronas, em ordem, sem paralelismo (R-02).
 *
 * ATENÇÃO (spec §4): sem try/catch, retry, cache, valor padrão ou fila.
 * Qualquer falha de dependência propaga e vira 500 no ApiExceptionHandler.
 *
 * Sem @Transactional aqui (D-07): o pool do SQLite tem 1 conexão, e uma
 * transação aberta durante o /sell (300-900 ms) serializaria todas as compras.
 * A transação cobre só o save, dentro do SaleService.
 */
@Service
public class PurchaseService {

    private final AirlinesHubClient airlines;
    private final ExchangeClient exchange;
    private final FidelityClient fidelity;
    private final SaleService saleService;

    public PurchaseService(AirlinesHubClient airlines, ExchangeClient exchange,
                           FidelityClient fidelity, SaleService saleService) {
        this.airlines = airlines;
        this.exchange = exchange;
        this.fidelity = fidelity;
        this.saleService = saleService;
    }

    public BuyTicketResponse buy(String flight, String day, String user, String correlationId) {
        // 1. consulta o voo (404 -> FlightNotFoundException)
        FlightInfo info = airlines.getFlight(flight, day);

        // 2. taxa de câmbio
        ExchangeRate rate = exchange.getRate();

        // 3. efetiva a venda (409 -> SoldOutException)
        SellResult sell = airlines.sell(flight, day);

        // 4. cálculos
        BigDecimal valueUSD = info.priceUSD();
        BigDecimal valueBRL = valueUSD.multiply(rate.rate()).setScale(2, RoundingMode.HALF_UP);
        int bonus = valueUSD.setScale(0, RoundingMode.FLOOR).intValueExact();

        // 5. bônus de fidelidade (no caminho crítico, de propósito)
        fidelity.addBonus(user, bonus);

        // 6. registro local: SÓ depois do passo 5.
        // Se o Fidelity falhar, o assento já foi consumido e não há linha em /sales.
        // Esse é o defeito que a Parte 1 quer observar (cenário O3): não corrigir.
        saleService.record(correlationId, sell.transactionId(), flight, day, user,
                valueUSD, valueBRL, Instant.now());

        return new BuyTicketResponse(sell.transactionId(), flight, day, user,
                valueUSD, valueBRL, rate.rate(), rate.asOf(), bonus);
    }
}