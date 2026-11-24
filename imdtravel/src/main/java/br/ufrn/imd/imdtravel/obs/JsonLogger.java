package br.ufrn.imd.imdtravel.obs;

import java.io.PrintStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Log estruturado: um objeto JSON por linha no stdout, sempre com
 * ts, svc, cid, event e ms (R-13). Campos extras vêm depois.
 */
@Component
public class JsonLogger {

    public static final String SERVICE = "imdtravel";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PrintStream out;

    @Autowired
    public JsonLogger() {
        this(System.out);
    }

    public JsonLogger(PrintStream out) {
        this.out = out;
    }

    /** Evento sem campos extras e sem duração. */
    public void log(String event) {
        log(event, 0, Map.of());
    }

    /** Evento com duração em ms e campos extras (na ordem do mapa). */
    public void log(String event, long ms, Map<String, ?> extras) {
        Map<String, Object> linha = new LinkedHashMap<>();
        linha.put("ts", Instant.now().toString());
        linha.put("svc", SERVICE);
        linha.put("cid", CorrelationContext.get());
        linha.put("event", event);
        linha.put("ms", ms);
        linha.putAll(extras);
        String json;
        try {
            json = MAPPER.writeValueAsString(linha);
        } catch (JsonProcessingException e) {
            // Só ocorre com valor não serializável; registra o evento mínimo.
            json = "{\"ts\":\"" + Instant.now() + "\",\"svc\":\"" + SERVICE
                    + "\",\"cid\":null,\"event\":\"log.falhou\",\"ms\":0}";
        }
        synchronized (out) {
            out.println(json);
            out.flush();
        }
    }

    /** Classe + mensagem de uma exceção, para o campo "erro". */
    public static String descrever(Throwable t) {
        return t.getClass().getName() + ": " + t.getMessage();
    }
}
