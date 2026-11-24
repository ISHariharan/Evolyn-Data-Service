package com.evolyn.evolyn_data_service.Goals;

import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal goal and dashboard endpoints (only Evolyn-API calls these; userId = the person asking):
 *   GET /evolyn/api/internal/tribes/{id}/dashboard?month=2026-11      goal, progress, daily totals, leaderboard
 *
 * The goal is the sum of the members' daily goals (see DailyGoalController), so there's
 * nothing to set here any more.
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal/tribes/{tribeId}", produces = "application/json")
public class TribeDashboardController {

    private final TribeDashboardService service;

    public TribeDashboardController(TribeDashboardService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@PathVariable UUID tribeId, @RequestParam UUID userId,
                                         @RequestParam(required = false) String month) {
        return service.dashboard(tribeId, userId, TribeDashboardService.parseMonth(month));
    }
}
