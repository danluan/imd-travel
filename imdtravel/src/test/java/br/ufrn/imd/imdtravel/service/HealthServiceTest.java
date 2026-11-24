package br.ufrn.imd.imdtravel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;

class HealthServiceTest {

    @TempDir Path dir;

    private JdbcTemplate jdbcOk() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        return jdbc;
    }

    @Test
    void okQuandoBancoEDiretorioFuncionam() throws IOException {
        HealthService.Result r = new HealthService(jdbcOk(), dir.toString()).check();

        assertThat(r.ok()).isTrue();
        try (var arquivos = Files.list(dir)) {
            assertThat(arquivos).isEmpty(); // o arquivo temporário foi apagado
        }
    }

    @Test
    void downQuandoDiretorioNaoExiste() {
        HealthService.Result r = new HealthService(jdbcOk(), dir.resolve("nao-existe").toString()).check();

        assertThat(r.ok()).isFalse();
        assertThat(r.reason()).contains("diretório");
    }

    @Test
    void downQuandoDiretorioNaoGravavel() {
        dir.toFile().setWritable(false);
        try {
            if (Files.isWritable(dir)) {
                return; // executando como root: a permissão não vale
            }
            HealthService.Result r = new HealthService(jdbcOk(), dir.toString()).check();
            assertThat(r.ok()).isFalse();
        } finally {
            dir.toFile().setWritable(true);
        }
    }

    @Test
    void downQuandoBancoFalha() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new CannotGetJdbcConnectionException("sem conexão"));

        HealthService.Result r = new HealthService(jdbc, dir.toString()).check();

        assertThat(r.ok()).isFalse();
        assertThat(r.reason()).startsWith("banco:");
    }
}
