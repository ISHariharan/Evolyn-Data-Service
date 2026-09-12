package com.evolyn.evolyn_data_service.Activities.Catalogue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal, read-only endpoints for the activity catalogue:
 *   GET /evolyn/api/internal/activity-definitions          every active activity, grouped by heading
 *   GET /evolyn/api/internal/activity-definitions/{code}   one activity, e.g. 15330
 *
 * The whole list is about 1,100 rows (~100 KB), small enough to send at once
 * and search in the browser.
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal/activity-definitions", produces = "application/json")
public class CatalogueController {

    public static final String ATTRIBUTION = "2024 Adult Compendium of Physical Activities "
            + "(Herrmann SD, et al. J Sport Health Sci 2024;13(1):6-12). https://pacompendium.com/";

    private final HeadingRepository headings;
    private final ActivityDefinitionRepository activities;

    public CatalogueController(HeadingRepository headings, ActivityDefinitionRepository activities) {
        this.headings = headings;
        this.activities = activities;
    }

    public record Activity(String code, String headingCode, BigDecimal met, String description) {
        static Activity of(ActivityDefinitionEntity e) {
            return new Activity(e.getActivityCode(), e.getHeadingCode(), e.getMetValue(), e.getDescription());
        }
    }

    public record Heading(String code, String name) {}

    public record Catalogue(String edition, String attribution, List<Heading> headings, List<Activity> activities) {}

    @GetMapping
    public Catalogue catalogue() {
        List<ActivityDefinitionEntity> rows = activities.findByActiveTrueOrderByActivityCodeAsc();
        String edition = rows.isEmpty() ? null : rows.get(0).getEdition();
        return new Catalogue(
                edition,
                ATTRIBUTION,
                headings.findAllByOrderByHeadingCodeAsc().stream().map(h -> new Heading(h.getHeadingCode(), h.getName())).toList(),
                rows.stream().map(Activity::of).toList());
    }

    @GetMapping("/{code}")
    public ResponseEntity<Object> one(@PathVariable String code) {
        return activities.findFirstByActivityCodeAndActiveTrue(code)
                .<ResponseEntity<Object>>map(e -> ResponseEntity.ok(Activity.of(e)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Unknown activity code " + code)));
    }
}
