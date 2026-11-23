package br.ufrn.imd.imdtravel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ufrn.imd.imdtravel.domain.Sale;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findAllByOrderByIdAsc();
}
