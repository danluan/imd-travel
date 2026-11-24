package br.ufrn.imd.imdtravel.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.ufrn.imd.imdtravel.controller.dto.SaleResponse;
import br.ufrn.imd.imdtravel.service.SaleService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SalesController.class)
@org.springframework.context.annotation.Import(br.ufrn.imd.imdtravel.obs.JsonLogger.class)
class SalesControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SaleService saleService;

    @Test
    void listaVaziaDevolveColchetes() throws Exception {
        when(saleService.listAll()).thenReturn(List.of());
        mvc.perform(get("/sales")).andExpect(status().isOk()).andExpect(content().json("[]", true));
    }

    @Test
    void camposExatosSemId() throws Exception {
        when(saleService.listAll()).thenReturn(List.of(new SaleResponse("cid-1", "tx-1", "IMD100",
                "2026-12-01", "ana", new BigDecimal("350.50"), new BigDecimal("1920.18"),
                Instant.parse("2026-11-20T16:29:59.468126826Z"))));
        mvc.perform(get("/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correlationId").value("cid-1"))
                .andExpect(jsonPath("$[0].transactionId").value("tx-1"))
                .andExpect(jsonPath("$[0].flight").value("IMD100"))
                .andExpect(jsonPath("$[0].day").value("2026-12-01"))
                .andExpect(jsonPath("$[0].user").value("ana"))
                .andExpect(jsonPath("$[0].valueUSD").value(350.50))
                .andExpect(jsonPath("$[0].valueBRL").value(1920.18))
                .andExpect(jsonPath("$[0].ts").value("2026-11-20T16:29:59.468126826Z"))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].length()").value(8));
    }
}
