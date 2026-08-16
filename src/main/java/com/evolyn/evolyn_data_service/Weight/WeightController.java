package com.evolyn.evolyn_data_service.Weight;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evolyn.evolyn_data_service.Common.EvolynTime;

/**
 * Internal endpoints for a person's own weight (only Evolyn-API calls these):
 *   PUT /evolyn/api/internal/weights                         save the weight for a day (replaces that day's)
 *   GET /evolyn/api/internal/weights?userId=&from=&to=       history, newest first (default: last 90 days)
 *   GET /evolyn/api/internal/weights/latest?userId=&onOrBefore=   the weight that applies on a day
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal/weights", produces = "application/json")
public class WeightController {

    static final BigDecimal MIN_KG = new BigDecimal("20");
    static final BigDecimal MAX_KG = new BigDecimal("300");

    private final WeightLogRepository weights;

    public WeightController(WeightLogRepository weights) {
        this.weights = weights;
    }

    public record WeightRequest(UUID userId, LocalDate recordedOn, BigDecimal weightKg) {}

    @PutMapping
    public ResponseEntity<Object> saveWeight(@RequestBody WeightRequest request) {
        if (request.userId() == null) return badRequest("userId is required");
        if (request.weightKg() == null) return badRequest("Enter your weight in kg");
        if (request.weightKg().compareTo(MIN_KG) < 0 || request.weightKg().compareTo(MAX_KG) > 0) {
            return badRequest("Weight must be between 20 and 300 kg");
        }
        LocalDate day = request.recordedOn() != null ? request.recordedOn() : EvolynTime.today();
        if (day.isAfter(EvolynTime.today())) return badRequest("You can't enter a weight for a future date");

        // One row per day: saving again on the same day corrects it
        WeightLogEntity entry = weights.findByUserIdAndRecordedOn(request.userId(), day).orElseGet(() -> {
            WeightLogEntity fresh = new WeightLogEntity();
            fresh.setWeightLogId(UUID.randomUUID());
            fresh.setUserId(request.userId());
            fresh.setRecordedOn(day);
            fresh.setCreatedAt(Instant.now());
            return fresh;
        });
        entry.setWeightKg(request.weightKg().setScale(2, java.math.RoundingMode.HALF_UP));
        entry.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(weights.save(entry));
    }

    @GetMapping
    public List<WeightLogEntity> history(@RequestParam("userId") UUID userId,
                                         @RequestParam(value = "from", required = false) LocalDate from,
                                         @RequestParam(value = "to", required = false) LocalDate to) {
        LocalDate end = to != null ? to : EvolynTime.today();
        LocalDate start = from != null ? from : end.minusDays(90);
        return weights.findByUserIdAndRecordedOnBetweenOrderByRecordedOnDesc(userId, start, end);
    }

    @GetMapping("/latest")
    public ResponseEntity<Object> latest(@RequestParam("userId") UUID userId,
                                         @RequestParam(value = "onOrBefore", required = false) LocalDate onOrBefore) {
        LocalDate day = onOrBefore != null ? onOrBefore : EvolynTime.today();
        return weights.findFirstByUserIdAndRecordedOnLessThanEqualOrderByRecordedOnDesc(userId, day)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No weight saved yet")));
    }

    private static ResponseEntity<Object> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }
}
