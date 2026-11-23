package br.ufrn.imd.imdtravel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import br.ufrn.imd.imdtravel.client.AirlinesHubClient;
import br.ufrn.imd.imdtravel.client.ExchangeClient;
import br.ufrn.imd.imdtravel.client.FidelityClient;
import br.ufrn.imd.imdtravel.client.dto.BonusResult;
import br.ufrn.imd.imdtravel.client.dto.ExchangeRate;
import br.ufrn.imd.imdtravel.client.dto.FlightInfo;
import br.ufrn.imd.imdtravel.client.dto.SellResult;
import br.ufrn.imd.imdtravel.controller.dto.BuyTicketResponse;
import br.ufrn.imd.imdtravel.service.exception.FlightNotFoundException;
import br.ufrn.imd.imdtravel.service.exception.SoldOutException;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    private static final String FLIGHT = "IMD100";
    private static final String DAY = "2026-12-01";
    private static final String USER = "ana";
    private static final String CID = "cid-1";

    @Mock AirlinesHubClient airlines;
    @Mock ExchangeClient exchange;
    @Mock FidelityClient fidelity;
    @Mock SaleService saleService;

    @InjectMocks PurchaseService service;

    private FlightInfo flightInfo(String price) {
        return new FlightInfo(FLIGHT, DAY, new BigDecimal(price), 20);
    }

    private ExchangeRate rate() {
        return new ExchangeRate(new BigDecimal("5.4784"), "2026-11-23T20:00:00Z");
    }

    @Test
    void caminhoFelizChamaNaOrdemECalculaValores() {
        when(airlines.getFlight(FLIGHT, DAY)).thenReturn(flightInfo("350.50"));
        when(exchange.getRate()).thenReturn(rate());
        when(airlines.sell(FLIGHT, DAY)).thenReturn(new SellResult("tx-1", 19));
        when(fidelity.addBonus(USER, 350)).thenReturn(new BonusResult(USER, 350));

        BuyTicketResponse resp = service.buy(FLIGHT, DAY, USER, CID);

        InOrder order = inOrder(airlines, exchange, fidelity, saleService);
        order.verify(airlines).getFlight(FLIGHT, DAY);
        order.verify(exchange).getRate();
        order.verify(airlines).sell(FLIGHT, DAY);
        order.verify(fidelity).addBonus(USER, 350);
        order.verify(saleService).record(eq(CID), eq("tx-1"), eq(FLIGHT), eq(DAY), eq(USER),
                eq(new BigDecimal("350.50")), eq(new BigDecimal("1920.18")), any(Instant.class));

        assertThat(resp.transactionId()).isEqualTo("tx-1");
        assertThat(resp.flight()).isEqualTo(FLIGHT);
        assertThat(resp.day()).isEqualTo(DAY);
        assertThat(resp.user()).isEqualTo(USER);
        assertThat(resp.valueUSD()).isEqualByComparingTo("350.50");
        assertThat(resp.valueBRL()).isEqualByComparingTo("1920.18");
        assertThat(resp.rate()).isEqualByComparingTo("5.4784");
        assertThat(resp.rateAsOf()).isEqualTo("2026-11-23T20:00:00Z");
        assertThat(resp.bonus()).isEqualTo(350);
    }

    @Test
    void bonusArredondaParaBaixo() {
        when(airlines.getFlight(FLIGHT, DAY)).thenReturn(flightInfo("99.99"));
        when(exchange.getRate()).thenReturn(rate());
        when(airlines.sell(FLIGHT, DAY)).thenReturn(new SellResult("tx-2", 18));
        when(fidelity.addBonus(USER, 99)).thenReturn(new BonusResult(USER, 99));

        BuyTicketResponse resp = service.buy(FLIGHT, DAY, USER, CID);

        assertThat(resp.bonus()).isEqualTo(99);
        verify(fidelity).addBonus(USER, 99);
    }

    @Test
    void falhaNoFlightNaoChamaMaisNada() {
        when(airlines.getFlight(FLIGHT, DAY)).thenThrow(new FlightNotFoundException(FLIGHT, DAY));

        assertThatThrownBy(() -> service.buy(FLIGHT, DAY, USER, CID))
                .isInstanceOf(FlightNotFoundException.class);

        verify(airlines, never()).sell(anyString(), anyString());
        verifyNoInteractions(exchange, fidelity, saleService);
    }

    @Test
    void falhaNoConvertNaoChamaSellNemRecord() {
        when(airlines.getFlight(FLIGHT, DAY)).thenReturn(flightInfo("350.50"));
        when(exchange.getRate()).thenThrow(new RestClientException("exchange fora do ar"));

        assertThatThrownBy(() -> service.buy(FLIGHT, DAY, USER, CID))
                .isInstanceOf(RestClientException.class);

        verify(airlines, never()).sell(anyString(), anyString());
        verifyNoInteractions(fidelity, saleService);
    }

    @Test
    void falhaNoSellNaoChamaFidelityNemRecord() {
        when(airlines.getFlight(FLIGHT, DAY)).thenReturn(flightInfo("350.50"));
        when(exchange.getRate()).thenReturn(rate());
        when(airlines.sell(FLIGHT, DAY)).thenThrow(new SoldOutException(FLIGHT, DAY));

        assertThatThrownBy(() -> service.buy(FLIGHT, DAY, USER, CID))
                .isInstanceOf(SoldOutException.class);

        verifyNoInteractions(fidelity, saleService);
    }

    @Test
    void falhaNoFidelityPropagaESemRegistroDeVenda() {
        when(airlines.getFlight(FLIGHT, DAY)).thenReturn(flightInfo("350.50"));
        when(exchange.getRate()).thenReturn(rate());
        when(airlines.sell(FLIGHT, DAY)).thenReturn(new SellResult("tx-3", 17));
        when(fidelity.addBonus(USER, 350)).thenThrow(new RestClientException("fidelity fora do ar"));

        assertThatThrownBy(() -> service.buy(FLIGHT, DAY, USER, CID))
                .isInstanceOf(RestClientException.class);

        // O assento já foi consumido no AirlinesHub, e nada foi gravado (cenário O3).
        verify(airlines).sell(FLIGHT, DAY);
        verify(saleService, never()).record(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), any(), any());
    }
}