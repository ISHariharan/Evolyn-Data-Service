package com.evolyn.evolyn_data_service.Goals;

import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal daily goal endpoints (only Evolyn-API calls these; userId = the person asking):
 *   GET /evolyn/api/internal/daily-goal     your goal, today's and this month's progress
 *   PUT /evolyn/api/internal/daily-goal     { "kcal": 400 }   applies from today
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal/daily-goal", produces = "application/json")
public class DailyGoalController {

    private final DailyGoalService service;

    public DailyGoalController(DailyGoalService service) {
        this.service = service;
    }

    public record GoalRequest(Integer kcal) {}

    @GetMapping
    public Map<String, Object> mine(@RequestParam UUID userId) {
        return service.mine(userId);
    }

    @PutMapping
    public Map<String, Object> set(@RequestParam UUID userId, @RequestBody GoalRequest request) {
        return service.set(userId, request == null ? null : request.kcal());
    }
}
