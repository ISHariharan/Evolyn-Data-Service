package com.evolyn.evolyn_data_service.Goals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * The arithmetic behind a goal, kept free of databases so it can be tested.
 *
 *   done %          = total / target x 100
 *   remaining       = max(target - total, 0)
 *   needed per day  = remaining / days left (today included)
 *   average per day = total / days gone so far (today included)
 *   at current pace = average per day x days in the period   (a projection, not a promise)
 */
public final class GoalMath {

    private GoalMath() {}

    public enum Phase { UPCOMING, ACTIVE, ENDED }

    public record Progress(
            Phase phase,
            BigDecimal total, BigDecimal target, BigDecimal percent, BigDecimal remaining,
            long totalDays, long elapsedDays, long remainingDays,
            BigDecimal averagePerDay,      // null before the period starts
            BigDecimal neededPerDay,       // null when nothing's left or the period is over
            BigDecimal projectedTotal,     // null unless the period is running
            BigDecimal projectedPercent,
            boolean reached) {}

    public static Progress progress(BigDecimal target, BigDecimal total, LocalDate start, LocalDate end, LocalDate today) {
        if (target == null || target.signum() <= 0) throw new IllegalArgumentException("target must be positive");
        if (end.isBefore(start)) throw new IllegalArgumentException("period ends before it starts");
        BigDecimal t = total == null ? BigDecimal.ZERO : total;

        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        Phase phase = today.isBefore(start) ? Phase.UPCOMING : today.isAfter(end) ? Phase.ENDED : Phase.ACTIVE;
        long elapsed = switch (phase) {
            case UPCOMING -> 0;
            case ENDED -> totalDays;
            case ACTIVE -> ChronoUnit.DAYS.between(start, today) + 1;   // today counts as a day gone
        };
        long left = switch (phase) {
            case UPCOMING -> totalDays;
            case ENDED -> 0;
            case ACTIVE -> ChronoUnit.DAYS.between(today, end) + 1;     // and as a day still to use
        };

        BigDecimal remaining = target.subtract(t).max(BigDecimal.ZERO);
        BigDecimal percent = t.multiply(BigDecimal.valueOf(100)).divide(target, 1, RoundingMode.HALF_UP);
        BigDecimal average = elapsed == 0 ? null : t.divide(BigDecimal.valueOf(elapsed), 2, RoundingMode.HALF_UP);
        BigDecimal needed = (left == 0 || remaining.signum() == 0) ? null
                : remaining.divide(BigDecimal.valueOf(left), 2, RoundingMode.HALF_UP);
        BigDecimal projected = phase == Phase.ACTIVE && average != null
                ? average.multiply(BigDecimal.valueOf(totalDays)).setScale(0, RoundingMode.HALF_UP) : null;
        BigDecimal projectedPercent = projected == null ? null
                : projected.multiply(BigDecimal.valueOf(100)).divide(target, 1, RoundingMode.HALF_UP);

        return new Progress(phase, t, target, percent, remaining, totalDays, elapsed, left,
                average, needed, projected, projectedPercent, t.compareTo(target) >= 0);
    }

    /**
     * Ranks with ties sharing a place: scores 900, 700, 700, 500 -> ranks 1, 2, 2, 4.
     * Returns the rank for each item in the order given after sorting highest first.
     */
    public static <T> List<Ranked<T>> rank(List<T> items, Function<T, BigDecimal> score) {
        List<T> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparing(score).reversed());
        List<Ranked<T>> out = new ArrayList<>();
        BigDecimal previous = null;
        int rank = 0;
        for (int i = 0; i < sorted.size(); i++) {
            BigDecimal s = score.apply(sorted.get(i));
            if (previous == null || s.compareTo(previous) != 0) rank = i + 1;
            previous = s;
            out.add(new Ranked<>(rank, sorted.get(i)));
        }
        return out;
    }

    public record Ranked<T>(int rank, T item) {}
}
