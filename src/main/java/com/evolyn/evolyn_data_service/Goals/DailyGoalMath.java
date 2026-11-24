package com.evolyn.evolyn_data_service.Goals;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Turns a person's daily goal history into targets, without any database.
 *
 * A goal applies from its effectiveFrom day until the next change. Days before
 * the person's first goal have no goal (0), so setting a goal mid-month only adds
 * the days that are left: earlier days are never rewritten.
 *
 *   person's month target = sum over the month's days of the goal on that day
 *   tribe's month target  = sum of its members' month targets
 */
public final class DailyGoalMath {

    private DailyGoalMath() {}

    public record Change(LocalDate effectiveFrom, int kcal) {}

    /** The goal on a given day, or 0 if none was set yet. */
    public static int goalOn(List<Change> changes, LocalDate day) {
        return changes.stream()
                .filter(c -> !c.effectiveFrom().isAfter(day))
                .max(Comparator.comparing(Change::effectiveFrom))
                .map(Change::kcal)
                .orElse(0);
    }

    /** Sum of the daily goals for every day from start to end, both included. */
    public static long target(List<Change> changes, LocalDate start, LocalDate end) {
        long sum = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            sum += goalOn(changes, d);
        }
        return sum;
    }
}
