package br.ufrn.imd.imdtravel.obs;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lê (ou gera) o X-Correlation-Id, devolve-o na resposta e registra
 * requisicao.recebida / resposta.enviada. GET /health com 200 não é logado.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private final JsonLogger logger;

    public CorrelationIdFilter(JsonLogger logger) {
        this.logger = logger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cid = request.getHeader(CorrelationContext.HEADER);
        if (cid == null || cid.isBlank()) {
            cid = UUID.randomUUID().toString();
        }
        CorrelationContext.set(cid);
        response.setHeader(CorrelationContext.HEADER, cid);

        String method = request.getMethod();
        String path = request.getRequestURI();
        boolean health = "/health".equals(path);
        long inicio = System.nanoTime();
        if (!health) {
            logRecebida(method, path);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            int status = response.getStatus();
            // /health só aparece no log quando não é 200.
            if (!health || status != 200) {
                if (health) {
                    logRecebida(method, path);
                }
                Map<String, Object> extras = new LinkedHashMap<>();
                extras.put("method", method);
                extras.put("path", path);
                extras.put("status", status);
                logger.log("resposta.enviada", ms, extras);
            }
            CorrelationContext.clear();
        }
    }

    private void logRecebida(String method, String path) {
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("method", method);
        extras.put("path", path);
        logger.log("requisicao.recebida", 0, extras);
    }
}
