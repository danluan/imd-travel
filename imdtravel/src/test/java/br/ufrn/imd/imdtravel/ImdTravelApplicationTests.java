package br.ufrn.imd.imdtravel;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import br.ufrn.imd.imdtravel.config.ImdTravelProperties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ImdTravelApplicationTests {

    static final Path DIR = tempDir();

    static Path tempDir() {
        try {
            return Files.createTempDirectory("imdtravel-ctx");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> "jdbc:sqlite:" + DIR.resolve("sales.db"));
    }

    @Autowired
    ImdTravelProperties props;

    @Test
    void contextoSobeEPropriedadesTemPadroes() {
        assertThat(props.airlineshubUrl()).isEqualTo("http://localhost:8081");
        assertThat(props.exchangeUrl()).isEqualTo("http://localhost:8082");
        assertThat(props.fidelityUrl()).isEqualTo("http://localhost:8083");
    }
}
