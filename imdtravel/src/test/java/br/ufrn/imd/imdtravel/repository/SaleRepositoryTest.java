package br.ufrn.imd.imdtravel.repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import br.ufrn.imd.imdtravel.domain.Sale;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SaleRepositoryTest {

    static final Path DIR = tempDir();

    static Path tempDir() {
        try {
            return Files.createTempDirectory("imdtravel-jpa");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> "jdbc:sqlite:" + DIR.resolve("sales.db"));
    }

    @Autowired
    SaleRepository repository;

    @Autowired
    TestEntityManager em;

    @Test
    void salvaListaEConfereIdaEVolta() {
        Instant ts = Instant.parse("2026-11-20T16:29:59.468126826Z");
        repository.save(new Sale("cid-1", "tx-1", "IMD100", "2026-12-01", "ana",
                new BigDecimal("350.50"), new BigDecimal("1920.18"), ts));
        repository.save(new Sale("cid-2", "tx-2", "IMD200", "2026-12-02", "bia",
                new BigDecimal("10"), new BigDecimal("55.1234"), ts.plusSeconds(1)));
        repository.flush();
        em.clear(); // força leitura do banco, não do cache de 1º nível

        List<Sale> todas = repository.findAllByOrderByIdAsc();
        assertThat(todas).hasSize(2);

        Sale s = todas.get(0);
        assertThat(s.getId()).isNotNull();
        assertThat(s.getCorrelationId()).isEqualTo("cid-1");
        assertThat(s.getTransactionId()).isEqualTo("tx-1");
        assertThat(s.getFlight()).isEqualTo("IMD100");
        assertThat(s.getDay()).isEqualTo("2026-12-01");
        assertThat(s.getUser()).isEqualTo("ana");
        assertThat(s.getValueUSD()).isEqualByComparingTo("350.50");
        assertThat(s.getValueBRL()).isEqualByComparingTo("1920.18");
        // o dialeto devolve 350.5 (escala perdida), mas o valor numérico é o mesmo
        System.out.println("ESCALA usd=" + s.getValueUSD() + " brl=" + s.getValueBRL() + " ts=" + s.getTs());
        // o dialeto SQLite grava Instant com precisão de milissegundos (nanos são truncados)
        assertThat(s.getTs()).isEqualTo(ts.truncatedTo(ChronoUnit.MILLIS));

        assertThat(todas.get(1).getValueBRL()).isEqualByComparingTo("55.1234");
        assertThat(todas.get(1).getTs()).isEqualTo(ts.plusSeconds(1).truncatedTo(ChronoUnit.MILLIS));
    }
}
