package br.ufrn.imd.imdtravel.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufrn.imd.imdtravel.controller.dto.SaleResponse;
import br.ufrn.imd.imdtravel.domain.Sale;
import br.ufrn.imd.imdtravel.repository.SaleRepository;

@Service
public class SaleService {

    private final SaleRepository repository;

    public SaleService(SaleRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Sale record(String correlationId, String transactionId, String flight, String day,
                       String user, BigDecimal valueUSD, BigDecimal valueBRL, Instant ts) {
        return repository.save(new Sale(correlationId, transactionId, flight, day,
                user, valueUSD, valueBRL, ts));
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> listAll() {
        return repository.findAllByOrderByIdAsc().stream().map(SaleResponse::from).toList();
    }
}
