package com.evolyn.evolyn_data_service.Streaks;

import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal streak endpoints (only Evolyn-API calls these; userId = the person asking):
 *   GET /evolyn/api/internal/tribes/{id}/streaks         tribe streak, every member's streak, the rules
 *   PUT /evolyn/api/internal/tribes/{id}/streak-rules    { "minMinutes": 20, "memberPercent": 50 }
 *   GET /evolyn/api/internal/streaks/me                  your own streak (default rule)
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal", produces = "application/json")
public class StreakController {

    private final StreakService service;

    public StreakController(StreakService service) {
        this.service = service;
    }

    public record RulesRequest(Integer minMinutes, Integer memberPercent) {}

    @GetMapping("/tribes/{tribeId}/streaks")
    public Map<String, Object> tribe(@PathVariable UUID tribeId, @RequestParam UUID userId) {
        return service.tribeStreaks(tribeId, userId);
    }

    @PutMapping("/tribes/{tribeId}/streak-rules")
    public Map<String, Object> rules(@PathVariable UUID tribeId, @RequestParam UUID userId, @RequestBody RulesRequest r) {
        return service.setRules(tribeId, userId, r == null ? null : r.minMinutes(), r == null ? null : r.memberPercent());
    }

    @GetMapping("/streaks/me")
    public Map<String, Object> mine(@RequestParam UUID userId) {
        return service.myStreak(userId);
    }
}
