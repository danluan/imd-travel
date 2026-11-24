package br.ufrn.imd.imdtravel.service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Verificação real de saúde (R-15): o SQLite responde a um SELECT 1 e o
 * diretório das vendas aceita escrita. Não é tolerância a falhas: só informa.
 */
@Service
public class HealthService {

    /** Resultado: ok ou down com o motivo. */
    public record Result(boolean ok, String reason) {
    }

    private final JdbcTemplate jdbc;
    private final Path salesDir;

    public HealthService(JdbcTemplate jdbc, @Value("${imdtravel.sales-dir}") String salesDir) {
        this.jdbc = jdbc;
        this.salesDir = Path.of(salesDir);
    }

    public Result check() {
        try {
            // Transação curtíssima: o pool tem uma única conexão (D-06).
            jdbc.queryForObject("SELECT 1", Integer.class);
        } catch (RuntimeException e) {
            return new Result(false, "banco: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        Path tmp = null;
        try {
            tmp = Files.createTempFile(salesDir, "health-", ".tmp");
        } catch (Exception e) {
            return new Result(false, "diretório de vendas não grava: " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
        } finally {
            if (tmp != null) {
                tmp.toFile().delete();
            }
        }
        return new Result(true, null);
    }
}
