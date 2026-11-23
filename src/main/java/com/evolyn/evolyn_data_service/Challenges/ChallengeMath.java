package com.evolyn.evolyn_data_service.Challenges;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.evolyn.evolyn_data_service.Goals.GoalMath;

/**
 * Challenge stages and scoring, free of databases so they can be tested.
 *
 * Stages: DRAFT -> (published) UPCOMING -> ACTIVE -> UNDER_REVIEW (the day after
 * it ends: in-period activities can still be logged or fixed) -> COMPLETED
 * (results frozen). CANCELLED can happen any time before COMPLETED.
 *
 * Scoring by type (ties always share a rank):
 *   COMPETITIVE        rank by total kcal
 *   COLLECTIVE         everyone's kcal adds to one shared target; people ranked by contribution
 *   INDIVIDUAL_TARGET  each person's kcal vs the same target; done when reached; ranked by kcal
 *   CONSISTENCY        number of different counting days; done when >= target days
 *   TEAM               team totals (A vs B) decide; individuals ranked by kcal within the results
 */
public final class ChallengeMath {

    private ChallengeMath() {}

    public enum Type { COMPETITIVE, COLLECTIVE, INDIVIDUAL_TARGET, CONSISTENCY, TEAM }

    public enum Phase { DRAFT, UPCOMING, ACTIVE, UNDER_REVIEW, COMPLETED, CANCELLED }

    public static Phase phase(String status, LocalDate start, LocalDate end, LocalDate today, boolean finalized) {
        if ("CANCELLED".equals(status)) return Phase.CANCELLED;
        if ("DRAFT".equals(status)) return Phase.DRAFT;
        if (finalized) return Phase.COMPLETED;
        if (today.isBefore(start)) return Phase.UPCOMING;
        if (!today.isAfter(end)) return Phase.ACTIVE;
        if (today.equals(end.plusDays(1))) return Phase.UNDER_REVIEW;
        return Phase.COMPLETED; // due to be frozen now
    }

    /** One participant's numbers for the challenge period. */
    public record Tally(UUID userId, String team, BigDecimal kcal, int days) {}

    public record PersonResult(UUID userId, String team, BigDecimal value, BigDecimal percent, boolean completed, int rank) {}

    public record TeamResult(String team, BigDecimal value, int members, int rank, boolean won) {}

    public record Standings(List<PersonResult> people, List<TeamResult> teams,
                            BigDecimal total, BigDecimal totalPercent, boolean completed) {}

    public static Standings score(Type type, BigDecimal target, List<Tally> tallies) {
        BigDecimal total = tallies.stream().map(Tally::kcal).reduce(BigDecimal.ZERO, BigDecimal::add);

        // What each person is ranked on
        java.util.function.Function<Tally, BigDecimal> value =
                type == Type.CONSISTENCY ? t -> BigDecimal.valueOf(t.days()) : Tally::kcal;

        List<PersonResult> people = new ArrayList<>();
        for (GoalMath.Ranked<Tally> r : GoalMath.rank(tallies, value)) {
            Tally t = r.item();
            BigDecimal v = value.apply(t);
            BigDecimal pct = null;
            boolean done = false;
            if (type == Type.INDIVIDUAL_TARGET || type == Type.CONSISTENCY) {
                pct = percent(v, target);
                done = target != null && v.compareTo(target) >= 0;
            } else if (type == Type.COLLECTIVE) {
                pct = total.signum() == 0 ? BigDecimal.ZERO : percent(v, total); // share of the shared total
            }
            people.add(new PersonResult(t.userId(), t.team(), v, pct, done, r.rank()));
        }

        List<TeamResult> teams = new ArrayList<>();
        if (type == Type.TEAM) {
            Map<String, BigDecimal> sum = new LinkedHashMap<>();
            Map<String, Integer> count = new HashMap<>();
            for (String team : List.of("A", "B")) {
                sum.put(team, BigDecimal.ZERO);
                count.put(team, 0);
            }
            for (Tally t : tallies) {
                if (t.team() == null) continue;
                sum.merge(t.team(), t.kcal(), BigDecimal::add);
                count.merge(t.team(), 1, Integer::sum);
            }
            List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(sum.entrySet());
            List<GoalMath.Ranked<Map.Entry<String, BigDecimal>>> ranked = GoalMath.rank(entries, Map.Entry::getValue);
            boolean tie = ranked.size() == 2 && ranked.get(0).rank() == ranked.get(1).rank();
            for (GoalMath.Ranked<Map.Entry<String, BigDecimal>> r : ranked) {
                String team = r.item().getKey();
                boolean won = !tie && r.rank() == 1 && r.item().getValue().signum() > 0;
                teams.add(new TeamResult(team, r.item().getValue(), count.get(team), r.rank(), won));
            }
        }

        BigDecimal totalPct = type == Type.COLLECTIVE ? percent(total, target) : null;
        boolean completed = type == Type.COLLECTIVE && target != null && total.compareTo(target) >= 0;
        return new Standings(people, teams, total, totalPct, completed);
    }

    private static BigDecimal percent(BigDecimal value, BigDecimal of) {
        if (of == null || of.signum() == 0) return null;
        return value.multiply(BigDecimal.valueOf(100)).divide(of, 1, RoundingMode.HALF_UP);
    }
}
