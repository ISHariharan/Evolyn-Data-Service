package com.evolyn.evolyn_data_service.Activities;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evolyn.evolyn_data_service.Activities.ActivityService.ActivityList;
import com.evolyn.evolyn_data_service.Activities.ActivityService.ActivityRequest;
import com.evolyn.evolyn_data_service.Activities.ActivityService.ActivityView;
import com.evolyn.evolyn_data_service.Activities.ActivityService.Problem;

/**
 * Internal endpoints for a person's own activities (only Evolyn-API calls these):
 *   POST   /evolyn/api/internal/activities
 *   GET    /evolyn/api/internal/activities?userId=&from=&to=    (default: this month so far)
 *   GET    /evolyn/api/internal/activities/{id}?userId=
 *   PUT    /evolyn/api/internal/activities/{id}
 *   DELETE /evolyn/api/internal/activities/{id}?userId=
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal/activities", produces = "application/json")
public class ActivityController {

    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ActivityView> create(@RequestBody ActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public ActivityList list(@RequestParam("userId") UUID userId,
                             @RequestParam(value = "from", required = false) LocalDate from,
                             @RequestParam(value = "to", required = false) LocalDate to) {
        return service.list(userId, from, to);
    }

    @GetMapping("/{activityId}")
    public ActivityView get(@PathVariable UUID activityId, @RequestParam("userId") UUID userId) {
        return service.get(activityId, userId);
    }

    @PutMapping("/{activityId}")
    public ActivityView update(@PathVariable UUID activityId, @RequestBody ActivityRequest request) {
        return service.update(activityId, request);
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> delete(@PathVariable UUID activityId, @RequestParam("userId") UUID userId) {
        service.delete(activityId, userId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Problem.class)
    public ResponseEntity<Object> problem(Problem p) {
        return ResponseEntity.status(p.status).body(p.body);
    }
}
