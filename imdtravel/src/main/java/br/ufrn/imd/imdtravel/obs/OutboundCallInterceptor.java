package br.ufrn.imd.imdtravel.obs;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Propaga o X-Correlation-Id nas chamadas de saída (R-12) e registra UM evento
 * por chamada: chamada.concluida ou chamada.falhou. Não altera o comportamento:
 * sem timeout, sem retry, e a exceção é relançada como veio.
 */
public class OutboundCallInterceptor implements ClientHttpRequestInterceptor {

    private final JsonLogger logger;
    private final String dest;

    public OutboundCallInterceptor(JsonLogger logger, String dest) {
        this.logger = logger;
        this.dest = dest;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String cid = CorrelationContext.get();
        if (cid != null) {
            request.getHeaders().set(CorrelationContext.HEADER, cid);
        }
        long inicio = System.nanoTime();
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("dest", dest);
        extras.put("method", request.getMethod().name());
        extras.put("path", request.getURI().getPath());
        try {
            ClientHttpResponse response = execution.execute(request, body);
            extras.put("status", response.getStatusCode().value());
            logger.log("chamada.concluida", ms(inicio), extras);
            return response;
        } catch (IOException e) {
            extras.put("erro", JsonLogger.descrever(e));
            logger.log("chamada.falhou", ms(inicio), extras);
            throw e;
        }
    }

    private static long ms(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}
