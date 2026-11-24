package br.ufrn.imd.imdtravel.controller;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ufrn.imd.imdtravel.service.HealthService;

@RestController
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        HealthService.Result r = healthService.check();
        Map<String, Object> body = new LinkedHashMap<>();
        if (r.ok()) {
            body.put("status", "ok");
            body.put("uptimeS", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
            return ResponseEntity.ok(body);
        }
        body.put("status", "down");
        body.put("reason", r.reason());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
