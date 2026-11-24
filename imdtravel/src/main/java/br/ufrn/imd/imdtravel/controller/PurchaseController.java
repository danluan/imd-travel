package br.ufrn.imd.imdtravel.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.ufrn.imd.imdtravel.controller.dto.BuyTicketRequest;
import br.ufrn.imd.imdtravel.controller.dto.BuyTicketResponse;
import br.ufrn.imd.imdtravel.obs.CorrelationContext;
import br.ufrn.imd.imdtravel.service.PurchaseService;
import jakarta.validation.Valid;

@RestController
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping("/buyTicket")
    public ResponseEntity<BuyTicketResponse> buyTicket(@Valid @RequestBody BuyTicketRequest request) {
        // O CorrelationIdFilter já leu ou gerou o cid e o devolve no cabeçalho.
        String correlationId = CorrelationContext.get();

        BuyTicketResponse body = purchaseService.buy(
                request.flight(), request.day(), request.user(), correlationId);

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
