package com.evolyn.evolyn_data_service.Weight;

import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evolyn.evolyn_data_service.Goals.TribeDashboardService;

/**
 * Internal weight-sharing endpoints (only Evolyn-API calls these; userId = the person asking):
 *   GET /evolyn/api/internal/tribes/{id}/weight-progress?month=2026-11   members' weight change this month
 *   GET /evolyn/api/internal/weight-sharing                              do you share your weight change?
 *   PUT /evolyn/api/internal/weight-sharing  { "shareWeightChange": true }
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal", produces = "application/json")
public class TribeWeightController {

    private final TribeWeightService service;

    public TribeWeightController(TribeWeightService service) {
        this.service = service;
    }

    public record SharingRequest(Boolean shareWeightChange) {}

    @GetMapping("/tribes/{tribeId}/weight-progress")
    public Map<String, Object> progress(@PathVariable UUID tribeId, @RequestParam UUID userId,
                                        @RequestParam(required = false) String month) {
        return service.progress(tribeId, userId, TribeDashboardService.parseMonth(month));
    }

    @GetMapping("/weight-sharing")
    public Map<String, Object> sharing(@RequestParam UUID userId) {
        return service.sharing(userId);
    }

    @PutMapping("/weight-sharing")
    public Map<String, Object> setSharing(@RequestParam UUID userId, @RequestBody SharingRequest r) {
        return service.setSharing(userId, r != null && Boolean.TRUE.equals(r.shareWeightChange()));
    }
}
