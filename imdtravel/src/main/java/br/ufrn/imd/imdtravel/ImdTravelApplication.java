package br.ufrn.imd.imdtravel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ImdTravelApplication {

    public static void main(String[] args) throws IOException {
        String dir = System.getenv().getOrDefault("SALES_DIR", "./data");
        Files.createDirectories(Path.of(dir));
        SpringApplication.run(ImdTravelApplication.class, args);
    }
}
