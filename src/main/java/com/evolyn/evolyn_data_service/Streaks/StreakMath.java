package com.evolyn.evolyn_data_service.Streaks;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Streak arithmetic, free of databases so it can be tested.
 *
 *   current : consecutive counting days ending today. If today doesn't count
 *             YET, the run ending yesterday is still alive ("at risk" until
 *             midnight), so a streak isn't shown as broken mid-morning.
 *   best    : the longest run ever.
 * Missed days are never forgiven (no streak freezes).
 */
public final class StreakMath {

    private StreakMath() {}

    public record Streak(int current, int best, boolean activeToday, boolean atRisk, LocalDate lastActive) {}

    public static Streak of(Collection<LocalDate> countingDays, LocalDate today) {
        TreeSet<LocalDate> days = new TreeSet<>(countingDays);
        days.removeIf(d -> d.isAfter(today)); // nothing from the future counts

        int best = 0, run = 0;
        LocalDate previous = null;
        for (LocalDate d : days) {
            run = (previous != null && d.equals(previous.plusDays(1))) ? run + 1 : 1;
            best = Math.max(best, run);
            previous = d;
        }

        boolean activeToday = days.contains(today);
        LocalDate cursor = activeToday ? today : today.minusDays(1);
        int current = 0;
        while (days.contains(cursor)) {
            current++;
            cursor = cursor.minusDays(1);
        }
        boolean atRisk = !activeToday && current > 0;
        return new Streak(current, best, activeToday, atRisk, days.isEmpty() ? null : days.last());
    }

    /** How many members must be active for the tribe's day to count: ceil(percent of members), at least 1. */
    public static int needed(int memberCount, int percent) {
        if (memberCount <= 0) return 1;
        return Math.max(1, (int) Math.ceil(memberCount * percent / 100.0));
    }

    /** The tribe's counting days: days on which at least `needed` members had a counting day. */
    public static Set<LocalDate> tribeDays(Map<LocalDate, Integer> activeMembersPerDay, int needed) {
        return activeMembersPerDay.entrySet().stream()
                .filter(e -> e.getValue() >= needed)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
