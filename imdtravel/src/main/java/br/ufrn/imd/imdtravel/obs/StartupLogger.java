package br.ufrn.imd.imdtravel.obs;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Registra app.iniciada quando a aplicação está pronta para receber requisições. */
@Component
public class StartupLogger {

    private final JsonLogger logger;

    public StartupLogger(JsonLogger logger) {
        this.logger = logger;
    }

    @EventListener(ApplicationReadyEvent.class)
    void aoFicarPronta() {
        logger.log("app.iniciada");
    }
}
