package com.evolyn.evolyn_data_service.Goals;

import java.math.BigDecimal;
import java.util.List;
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
 * Internal goal and dashboard endpoints (only Evolyn-API calls these; userId = the person asking):
 *   GET /evolyn/api/internal/tribes/{id}/dashboard?month=2026-11      goal, progress, daily totals, leaderboard
 *   PUT /evolyn/api/internal/tribes/{id}/goals/{month}                 set the month's target { "target": 60000 }
 *   GET /evolyn/api/internal/tribes/{id}/goals/{month}/changes         who set or changed it
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal/tribes/{tribeId}", produces = "application/json")
public class TribeDashboardController {

    private final TribeDashboardService service;

    public TribeDashboardController(TribeDashboardService service) {
        this.service = service;
    }

    public record GoalRequest(BigDecimal target) {}

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@PathVariable UUID tribeId, @RequestParam UUID userId,
                                         @RequestParam(required = false) String month) {
        return service.dashboard(tribeId, userId, TribeDashboardService.parseMonth(month));
    }

    @PutMapping("/goals/{month}")
    public Map<String, Object> setGoal(@PathVariable UUID tribeId, @PathVariable String month,
                                       @RequestParam UUID userId, @RequestBody GoalRequest request) {
        return service.setGoal(tribeId, userId, TribeDashboardService.parseMonth(month),
                request == null ? null : request.target());
    }

    @GetMapping("/goals/{month}/changes")
    public List<Map<String, Object>> changes(@PathVariable UUID tribeId, @PathVariable String month,
                                             @RequestParam UUID userId) {
        return service.goalHistory(tribeId, userId, TribeDashboardService.parseMonth(month));
    }
}
