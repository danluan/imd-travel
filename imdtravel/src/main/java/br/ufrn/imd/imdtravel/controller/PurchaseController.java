package br.ufrn.imd.imdtravel.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import br.ufrn.imd.imdtravel.controller.dto.BuyTicketRequest;
import br.ufrn.imd.imdtravel.controller.dto.BuyTicketResponse;
import br.ufrn.imd.imdtravel.service.PurchaseService;
import jakarta.validation.Valid;

@RestController
public class PurchaseController {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping("/buyTicket")
    public ResponseEntity<BuyTicketResponse> buyTicket(
            @Valid @RequestBody BuyTicketRequest request,
            @RequestHeader(value = CORRELATION_HEADER, required = false) String incomingCid) {

        // Mínimo da Fase 4: aproveita o cid do cliente ou gera um. A propagação
        // completa e o filtro vêm na Fase 5.
        String correlationId = (incomingCid == null || incomingCid.isBlank())
                ? UUID.randomUUID().toString()
                : incomingCid;

        BuyTicketResponse body = purchaseService.buy(
                request.flight(), request.day(), request.user(), correlationId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(CORRELATION_HEADER, correlationId)
                .body(body);
    }
}