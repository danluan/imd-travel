package br.ufrn.imd.imdtravel.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;

import br.ufrn.imd.imdtravel.controller.dto.BuyTicketResponse;
import br.ufrn.imd.imdtravel.obs.CorrelationIdFilter;
import br.ufrn.imd.imdtravel.obs.JsonLogger;
import br.ufrn.imd.imdtravel.service.PurchaseService;
import br.ufrn.imd.imdtravel.service.exception.FlightNotFoundException;
import br.ufrn.imd.imdtravel.service.exception.SoldOutException;

@WebMvcTest(PurchaseController.class)
@Import({CorrelationIdFilter.class, JsonLogger.class, ApiExceptionHandler.class})
class PurchaseControllerTest {

    private static final String BODY_OK =
            "{\"flight\":\"IMD100\",\"day\":\"2026-12-01\",\"user\":\"ana\"}";

    @Autowired MockMvc mvc;

    @MockitoBean PurchaseService purchaseService;

    private BuyTicketResponse resposta() {
        return new BuyTicketResponse("tx-1", "IMD100", "2026-12-01", "ana",
                new BigDecimal("350.50"), new BigDecimal("1920.18"),
                new BigDecimal("5.4784"), "2026-11-23T20:00:00Z", 350);
    }

    @Test
    void devolve201ComOsNoveCampos() throws Exception {
        when(purchaseService.buy(eq("IMD100"), eq("2026-12-01"), eq("ana"), anyString()))
                .thenReturn(resposta());

        mvc.perform(post("/buyTicket").contentType(MediaType.APPLICATION_JSON).content(BODY_OK))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.flight").value("IMD100"))
                .andExpect(jsonPath("$.day").value("2026-12-01"))
                .andExpect(jsonPath("$.user").value("ana"))
                .andExpect(jsonPath("$.valueUSD").value(350.5))
                .andExpect(jsonPath("$.valueBRL").value(1920.18))
                .andExpect(jsonPath("$.rate").value(5.4784))
                .andExpect(jsonPath("$.rateAsOf").value("2026-11-23T20:00:00Z"))
                .andExpect(jsonPath("$.bonus").value(350));
    }

    @Test
    void aproveitaOCorrelationIdDoCliente() throws Exception {
        when(purchaseService.buy(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(resposta());

        mvc.perform(post("/buyTicket")
                        .header("X-Correlation-Id", "abc-123")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_OK))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", "abc-123"));

        verify(purchaseService).buy("IMD100", "2026-12-01", "ana", "abc-123");
    }

    @Test
    void semDiaDevolve400() throws Exception {
        mvc.perform(post("/buyTicket").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flight\":\"IMD100\",\"user\":\"ana\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid-request"));
    }

    @Test
    void userVazioDevolve400() throws Exception {
        mvc.perform(post("/buyTicket").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flight\":\"IMD100\",\"day\":\"2026-12-01\",\"user\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid-request"));
    }

    @Test
    void jsonMalformadoDevolve400() throws Exception {
        mvc.perform(post("/buyTicket").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flight\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid-request"));
    }

    @Test
    void vooInexistenteDevolve404() throws Exception {
        when(purchaseService.buy(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new FlightNotFoundException("IMD100", "2026-12-01"));

        mvc.perform(post("/buyTicket").contentType(MediaType.APPLICATION_JSON).content(BODY_OK))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not-found"));
    }

    @Test
    void vooEsgotadoDevolve409() throws Exception {
        when(purchaseService.buy(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new SoldOutException("IMD100", "2026-12-01"));

        mvc.perform(post("/buyTicket").contentType(MediaType.APPLICATION_JSON).content(BODY_OK))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("sold-out"));
    }

    @Test
    void falhaDeDependenciaDevolve500() throws Exception {
        when(purchaseService.buy(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RestClientException("dependencia fora do ar"));

        mvc.perform(post("/buyTicket").contentType(MediaType.APPLICATION_JSON).content(BODY_OK))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("internal-error"));
    }

    @Test
    void geraCorrelationIdQuandoAusente() throws Exception {
        when(purchaseService.buy(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(resposta());

        String cid = mvc.perform(post("/buyTicket").contentType(MediaType.APPLICATION_JSON).content(BODY_OK))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("X-Correlation-Id");

        org.assertj.core.api.Assertions.assertThat(java.util.UUID.fromString(cid)).isNotNull();
        verify(purchaseService).buy("IMD100", "2026-12-01", "ana", cid);
    }

    @Test
    void correlationIdVoltaTambemNoErro500() throws Exception {
        when(purchaseService.buy(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RestClientException("fora do ar"));

        mvc.perform(post("/buyTicket")
                        .header("X-Correlation-Id", "cid-500")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_OK))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Correlation-Id", "cid-500"));
    }

    @Test
    void correlationIdVoltaTambemNo400() throws Exception {
        mvc.perform(post("/buyTicket")
                        .header("X-Correlation-Id", "cid-400")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"flight\":"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Correlation-Id", "cid-400"));
    }
}
