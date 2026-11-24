package br.ufrn.imd.imdtravel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import br.ufrn.imd.imdtravel.obs.JsonLogger;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ImdTravelApplication {

    public static void main(String[] args) throws IOException {
        String dir = System.getenv().getOrDefault("SALES_DIR", "./data");
        Files.createDirectories(Path.of(dir));
        try {
            SpringApplication.run(ImdTravelApplication.class, args);
        } catch (Throwable e) {
            // Só para a falha de subida não ficar invisível (o log de texto está desligado).
            new JsonLogger().log("app.falhou", 0, Map.of("erro", JsonLogger.descrever(e)));
            throw e;
        }
    }

}
