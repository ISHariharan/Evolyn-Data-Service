package com.evolyn.evolyn_data_service.Security;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Render calls GET /health to know the service has started and is alive. */
@RestController
public class HealthController {

    @GetMapping(value = "/health", produces = "application/json")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
