package com.evolyn.evolyn_data_service.Challenges;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal challenge endpoints (only Evolyn-API calls these; userId = the person asking):
 *   GET  /tribes/{tribeId}/challenges            the tribe's challenges (your drafts included)
 *   POST /tribes/{tribeId}/challenges            create { name, type, startDate, endDate, target?, minMinutes?, headingCode?, publish? }
 *   GET  /challenges/{id}                        standings, your activities, what you can do
 *   PUT  /challenges/{id}                        edit (rules only before it starts)
 *   POST /challenges/{id}/publish | cancel | leave
 *   POST /challenges/{id}/join                   { "team": "A" } for team challenges
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal", produces = "application/json")
public class ChallengeController {

    private final ChallengeService service;

    public ChallengeController(ChallengeService service) {
        this.service = service;
    }

    public record JoinRequest(String team) {}

    @GetMapping("/tribes/{tribeId}/challenges")
    public List<Map<String, Object>> list(@PathVariable UUID tribeId, @RequestParam UUID userId) {
        return service.list(tribeId, userId);
    }

    @PostMapping("/tribes/{tribeId}/challenges")
    public ResponseEntity<Map<String, Object>> create(@PathVariable UUID tribeId, @RequestParam UUID userId,
                                                      @RequestBody ChallengeService.ChallengeRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(tribeId, userId, r));
    }

    @GetMapping("/challenges/{challengeId}")
    public Map<String, Object> detail(@PathVariable UUID challengeId, @RequestParam UUID userId) {
        return service.detail(challengeId, userId);
    }

    @PutMapping("/challenges/{challengeId}")
    public Map<String, Object> update(@PathVariable UUID challengeId, @RequestParam UUID userId,
                                      @RequestBody ChallengeService.ChallengeRequest r) {
        return service.update(challengeId, userId, r);
    }

    @PostMapping("/challenges/{challengeId}/publish")
    public Map<String, Object> publish(@PathVariable UUID challengeId, @RequestParam UUID userId) {
        return service.publish(challengeId, userId);
    }

    @PostMapping("/challenges/{challengeId}/cancel")
    public Map<String, Object> cancel(@PathVariable UUID challengeId, @RequestParam UUID userId) {
        return service.cancel(challengeId, userId);
    }

    @PostMapping("/challenges/{challengeId}/join")
    public Map<String, Object> join(@PathVariable UUID challengeId, @RequestParam UUID userId,
                                    @RequestBody(required = false) JoinRequest r) {
        return service.join(challengeId, userId, r == null ? null : r.team());
    }

    @PostMapping("/challenges/{challengeId}/leave")
    public Map<String, Object> leave(@PathVariable UUID challengeId, @RequestParam UUID userId) {
        return service.leave(challengeId, userId);
    }
}
