package br.ufrn.imd.imdtravel.obs;

import org.slf4j.MDC;

/**
 * Guarda o X-Correlation-Id da requisição em andamento (ThreadLocal + MDC).
 * A compra é síncrona numa única thread, então o interceptor HTTP de saída e o
 * logger enxergam o mesmo valor que o filtro gravou.
 */
public final class CorrelationContext {

    public static final String HEADER = "X-Correlation-Id";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationContext() {
    }

    public static void set(String cid) {
        CURRENT.set(cid);
        MDC.put("cid", cid);
    }

    /** cid atual, ou null fora de uma requisição. */
    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
        MDC.remove("cid");
    }
}
