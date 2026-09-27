package com.evolyn.evolyn_data_service.Activities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evolyn.evolyn_data_service.Activities.Catalogue.ActivityDefinitionEntity;
import com.evolyn.evolyn_data_service.Activities.Catalogue.ActivityDefinitionRepository;
import com.evolyn.evolyn_data_service.Common.EvolynTime;
import com.evolyn.evolyn_data_service.Weight.WeightLogEntity;
import com.evolyn.evolyn_data_service.Weight.WeightLogRepository;

/**
 * Logging, editing and deleting your own activities.
 *
 * Every save works out the calories on the server (never trusted from the
 * browser) and stores the inputs used, so the number can always be explained.
 */
@Service
public class ActivityService {

    static final int MAX_MINUTES = 24 * 60;
    static final Set<String> VISIBILITIES = Set.of("TRIBES", "PRIVATE");

    private final UserActivityRepository activities;
    private final ActivityCalculationRepository calculations;
    private final ActivityDefinitionRepository definitions;
    private final WeightLogRepository weights;

    public ActivityService(UserActivityRepository activities, ActivityCalculationRepository calculations,
                           ActivityDefinitionRepository definitions, WeightLogRepository weights) {
        this.activities = activities;
        this.calculations = calculations;
        this.definitions = definitions;
        this.weights = weights;
    }

    // ── What the browser sends ───────────────────────────────────────────────
    /**
     * startedAt is India local time, e.g. "2026-11-22T18:30".
     * allowOverlap: set after the person confirms an overlapping activity is really separate.
     */
    public record ActivityRequest(UUID userId, String activityCode, LocalDateTime startedAt, Integer durationMinutes,
                                  BigDecimal distanceKm, String notes, String visibility, Boolean allowOverlap) {}

    // ── What it gets back ────────────────────────────────────────────────────
    public record Calculation(BigDecimal kcal, BigDecimal met, BigDecimal weightKg, LocalDate weightRecordedOn,
                              BigDecimal hours, String formula, String edition, Instant calculatedAt) {}

    public record ActivityView(UUID id, String activityCode, String headingCode, String description,
                               LocalDateTime startedAt, LocalDate activityDate, int durationMinutes,
                               BigDecimal distanceKm, String notes, String visibility, String source,
                               BigDecimal kcal, Calculation calculation) {}

    public record ActivityList(LocalDate from, LocalDate to, int count, int activeDays, BigDecimal totalKcal,
                               int totalMinutes, List<ActivityView> activities) {}

    /** A request that can't be done, with the HTTP status and message to send back. */
    public static class Problem extends RuntimeException {
        final HttpStatus status;
        final Map<String, Object> body;

        Problem(HttpStatus status, String message) { this(status, Map.of("message", message)); }

        Problem(HttpStatus status, Map<String, Object> body) {
            super(String.valueOf(body.get("message")));
            this.status = status;
            this.body = body;
        }
    }

    // ── Operations ───────────────────────────────────────────────────────────
    @Transactional
    public ActivityView create(ActivityRequest request) {
        Checked c = check(request, null);
        UserActivityEntity activity = new UserActivityEntity();
        activity.setActivityId(UUID.randomUUID());
        activity.setUserId(request.userId());
        activity.setSource("MANUAL");
        activity.setCreatedAt(Instant.now());
        apply(activity, request, c);
        activities.save(activity);
        ActivityCalculationEntity calc = calculate(activity, null);
        return view(activity, calc);
    }

    @Transactional
    public ActivityView update(UUID activityId, ActivityRequest request) {
        UserActivityEntity activity = find(activityId, request.userId());
        Checked c = check(request, activity);
        ActivityCalculationEntity old = calculations
                .findByActivityIdAndMetricTypeAndCurrentTrue(activityId, CalorieCalculator.METRIC).orElse(null);

        // Only a change to what, when or how long changes the calories
        boolean recalc = old == null
                || !activity.getDefinition().getActivityDefinitionId().equals(c.definition.getActivityDefinitionId())
                || activity.getDurationSeconds() != c.durationSeconds
                || !activity.getActivityDate().equals(c.activityDate);
        apply(activity, request, c);
        activities.save(activity);
        ActivityCalculationEntity calc = recalc ? calculate(activity, old) : old;
        return view(activity, calc);
    }

    @Transactional
    public void delete(UUID activityId, UUID userId) {
        UserActivityEntity activity = find(activityId, userId);
        // Kept (with its calculation) for history, just hidden everywhere
        activity.setDeletedAt(Instant.now());
        activity.setUpdatedAt(Instant.now());
        activities.save(activity);
    }

    @Transactional(readOnly = true)
    public ActivityView get(UUID activityId, UUID userId) {
        UserActivityEntity activity = find(activityId, userId);
        return view(activity, calculations
                .findByActivityIdAndMetricTypeAndCurrentTrue(activityId, CalorieCalculator.METRIC).orElse(null));
    }

    @Transactional(readOnly = true)
    public ActivityList list(UUID userId, LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : EvolynTime.today();
        LocalDate start = from != null ? from : end.withDayOfMonth(1);
        if (start.isAfter(end)) throw new Problem(HttpStatus.BAD_REQUEST, "from must be on or before to");

        List<UserActivityEntity> rows = activities
                .findByUserIdAndDeletedAtIsNullAndActivityDateBetweenOrderByStartedAtDesc(userId, start, end);
        Map<UUID, ActivityCalculationEntity> calcs = rows.isEmpty() ? Map.of() : calculations
                .findByActivityIdInAndMetricTypeAndCurrentTrue(
                        rows.stream().map(UserActivityEntity::getActivityId).toList(), CalorieCalculator.METRIC)
                .stream().collect(Collectors.toMap(ActivityCalculationEntity::getActivityId, Function.identity()));

        List<ActivityView> views = rows.stream().map(a -> view(a, calcs.get(a.getActivityId()))).toList();
        BigDecimal total = views.stream().map(ActivityView::kcal).filter(k -> k != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int minutes = views.stream().mapToInt(ActivityView::durationMinutes).sum();
        int days = (int) rows.stream().map(UserActivityEntity::getActivityDate).distinct().count();
        return new ActivityList(start, end, views.size(), days, total.setScale(0, RoundingMode.HALF_UP), minutes, views);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private record Checked(ActivityDefinitionEntity definition, Instant startedAt, LocalDate activityDate,
                           int durationSeconds, String visibility) {}

    /** existing: the activity being edited, or null for a new one. */
    private Checked check(ActivityRequest r, UserActivityEntity existing) {
        if (r.userId() == null) throw new Problem(HttpStatus.BAD_REQUEST, "userId is required");
        if (r.activityCode() == null || r.activityCode().isBlank()) throw new Problem(HttpStatus.BAD_REQUEST, "Choose an activity");
        if (r.startedAt() == null) throw new Problem(HttpStatus.BAD_REQUEST, "Choose when you started");
        if (r.durationMinutes() == null || r.durationMinutes() < 1 || r.durationMinutes() > MAX_MINUTES) {
            throw new Problem(HttpStatus.BAD_REQUEST, "Duration must be between 1 minute and 24 hours");
        }
        if (r.distanceKm() != null && (r.distanceKm().signum() < 0 || r.distanceKm().compareTo(BigDecimal.valueOf(1000)) > 0)) {
            throw new Problem(HttpStatus.BAD_REQUEST, "Distance must be between 0 and 1,000 km");
        }
        if (r.notes() != null && r.notes().length() > 500) throw new Problem(HttpStatus.BAD_REQUEST, "Notes can be at most 500 characters");
        String visibility = r.visibility() == null ? "TRIBES" : r.visibility();
        if (!VISIBILITIES.contains(visibility)) throw new Problem(HttpStatus.BAD_REQUEST, "visibility must be TRIBES or PRIVATE");

        ActivityDefinitionEntity definition = definitions.findFirstByActivityCodeAndActiveTrue(r.activityCode().trim())
                .orElseThrow(() -> new Problem(HttpStatus.BAD_REQUEST, "Unknown activity " + r.activityCode()));

        Instant startedAt = r.startedAt().atZone(EvolynTime.ZONE).toInstant();
        if (startedAt.isAfter(Instant.now().plus(Duration.ofMinutes(10)))) {
            throw new Problem(HttpStatus.BAD_REQUEST, "You can't log an activity that hasn't started yet");
        }
        int seconds = r.durationMinutes() * 60;

        // The same person can't be doing two things at once, unless they confirm it.
        // When editing, only ask again if the time or duration changed.
        boolean timeChanged = existing == null
                || !existing.getStartedAt().equals(startedAt) || existing.getDurationSeconds() != seconds;
        if (timeChanged && !Boolean.TRUE.equals(r.allowOverlap())) {
            List<UserActivityEntity> overlapping = activities.findOverlapping(r.userId(), startedAt,
                    startedAt.plusSeconds(seconds), existing != null ? existing.getActivityId() : new UUID(0, 0));
            if (!overlapping.isEmpty()) {
                UserActivityEntity o = overlapping.get(0);
                throw new Problem(HttpStatus.CONFLICT, Map.of(
                        "code", "OVERLAP",
                        "message", "This overlaps with " + o.getDefinition().getDescription() + " at "
                                + localTime(o.getStartedAt()).toLocalTime() + " on " + o.getActivityDate()
                                + ". Is it a separate activity?",
                        "overlappingActivityId", o.getActivityId()));
            }
        }
        LocalDate activityDate = r.startedAt().toLocalDate();
        return new Checked(definition, startedAt, activityDate, seconds, visibility);
    }

    private static void apply(UserActivityEntity a, ActivityRequest r, Checked c) {
        a.setDefinition(c.definition);
        a.setStartedAt(c.startedAt);
        a.setActivityDate(c.activityDate);
        a.setDurationSeconds(c.durationSeconds);
        a.setDistanceKm(r.distanceKm() == null ? null : r.distanceKm().setScale(2, RoundingMode.HALF_UP));
        a.setNotes(r.notes() == null || r.notes().isBlank() ? null : r.notes().trim());
        a.setVisibility(c.visibility);
        a.setUpdatedAt(Instant.now());
    }

    private ActivityCalculationEntity calculate(UserActivityEntity a, ActivityCalculationEntity previous) {
        WeightLogEntity weight = weights
                .findFirstByUserIdAndRecordedOnLessThanEqualOrderByRecordedOnDesc(a.getUserId(), a.getActivityDate())
                .orElseThrow(() -> new Problem(HttpStatus.CONFLICT, Map.of(
                        "code", "WEIGHT_REQUIRED",
                        "message", "Save your weight first: calories are worked out from it")));

        if (previous != null) {
            previous.setCurrent(false);
            calculations.saveAndFlush(previous); // before inserting the new current one
        }
        ActivityDefinitionEntity d = a.getDefinition();
        ActivityCalculationEntity calc = new ActivityCalculationEntity();
        calc.setCalculationId(UUID.randomUUID());
        calc.setActivityId(a.getActivityId());
        calc.setMetricType(CalorieCalculator.METRIC);
        calc.setUnit(CalorieCalculator.UNIT);
        calc.setFormulaVersion(CalorieCalculator.FORMULA_VERSION);
        calc.setMetValue(d.getMetValue());
        calc.setWeightKgUsed(weight.getWeightKg());
        calc.setWeightRecordedOn(weight.getRecordedOn());
        calc.setDurationSeconds(a.getDurationSeconds());
        calc.setReferenceEdition(d.getEdition());
        calc.setReferenceActivityCode(d.getActivityCode());
        calc.setMetricValue(CalorieCalculator.kcal(d.getMetValue(), weight.getWeightKg(), a.getDurationSeconds()));
        calc.setCurrent(true);
        calc.setCalculatedAt(Instant.now());
        return calculations.save(calc);
    }

    private UserActivityEntity find(UUID activityId, UUID userId) {
        if (userId == null) throw new Problem(HttpStatus.BAD_REQUEST, "userId is required");
        return activities.findByActivityIdAndUserIdAndDeletedAtIsNull(activityId, userId)
                .orElseThrow(() -> new Problem(HttpStatus.NOT_FOUND, "Activity not found"));
    }

    private static LocalDateTime localTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, EvolynTime.ZONE);
    }

    private static ActivityView view(UserActivityEntity a, ActivityCalculationEntity c) {
        ActivityDefinitionEntity d = a.getDefinition();
        Calculation calc = c == null ? null : new Calculation(
                c.getMetricValue(), c.getMetValue(), c.getWeightKgUsed(), c.getWeightRecordedOn(),
                BigDecimal.valueOf(c.getDurationSeconds()).divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP),
                c.getFormulaVersion(), c.getReferenceEdition(), c.getCalculatedAt());
        return new ActivityView(a.getActivityId(), d.getActivityCode(), d.getHeadingCode(), d.getDescription(),
                localTime(a.getStartedAt()), a.getActivityDate(), a.getDurationSeconds() / 60, a.getDistanceKm(),
                a.getNotes(), a.getVisibility(), a.getSource(),
                c == null ? null : c.getMetricValue().setScale(0, RoundingMode.HALF_UP), calc);
    }
}
