package br.ufrn.imd.imdtravel.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ufrn.imd.imdtravel.controller.dto.SaleResponse;
import br.ufrn.imd.imdtravel.service.SaleService;

@RestController
public class SalesController {

    private final SaleService saleService;

    public SalesController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping("/sales")
    public List<SaleResponse> sales() {
        return saleService.listAll();
    }
}
