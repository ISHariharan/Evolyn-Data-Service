package com.evolyn.evolyn_data_service.Goals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evolyn.evolyn_data_service.Common.ApiProblem;
import com.evolyn.evolyn_data_service.Common.EvolynTime;
import com.evolyn.evolyn_data_service.Tribes.TribeService;

/**
 * A tribe's month: its calorie goal, progress, daily totals and leaderboard.
 *
 * What counts towards it:
 *   - activities of the tribe's CURRENT members, dated in the month (India time)
 *   - only activity types whose heading counts for goals (no sleeping, driving...)
 *   - "only me" activities too: they add to totals but never appear in the feed
 *   - the current calorie calculation of each activity; deleted ones never
 * Nothing here is stored: it's added up from the activities every time, so an
 * edited or deleted activity is reflected immediately.
 *
 * The month's goal isn't typed in: it's the sum of the members' daily goals over the
 * month's days (DailyGoalMath). Someone who sets or changes their daily goal changes
 * the tribe's target from that day on; earlier days keep what they had.
 */
@Service
public class TribeDashboardService {

    static final String METRIC = "CALORIES_BURNED";
    static final String UNIT = "kcal";

    private final TribeService tribes;
    private final NamedParameterJdbcTemplate sql;
    private final DailyGoalService dailyGoals;

    public TribeDashboardService(TribeService tribes, NamedParameterJdbcTemplate sql, DailyGoalService dailyGoals) {
        this.tribes = tribes;
        this.sql = sql;
        this.dailyGoals = dailyGoals;
    }

    /** "2026-11" -> November 2026; null -> this month in India. */
    public static YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) return YearMonth.from(EvolynTime.today());
        try {
            return YearMonth.parse(month);
        } catch (RuntimeException e) {
            throw ApiProblem.of(HttpStatus.BAD_REQUEST, "month must look like 2026-11");
        }
    }

    // ── The month's numbers ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(UUID tribeId, UUID userId, YearMonth month) {
        tribes.requireMember(tribeId, userId, false);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        LocalDate today = EvolynTime.today();
        MapSqlParameterSource p = new MapSqlParameterSource("tribeId", tribeId)
                .addValue("metric", METRIC)
                .addValue("start", Date.valueOf(start))
                .addValue("end", Date.valueOf(end));

        // Current members, in joining order
        List<Map<String, Object>> members = sql.queryForList("""
                SELECT u.uuid AS user_id, u.first_name, u.last_name, u.profile_id
                FROM tribe_members m JOIN evolyn_user_details u ON u.uuid = m.user_id
                WHERE m.tribe_id = :tribeId AND m.status = 'ACTIVE'
                ORDER BY m.joined_at
                """, p);

        // Calories per member per day, counting only headings that count for goals
        List<Map<String, Object>> rows = sql.queryForList("""
                SELECT a.user_id, a.activity_date, sum(c.metric_value) AS kcal,
                       count(*) AS sessions, sum(a.duration_seconds) AS seconds
                FROM user_activities a
                JOIN tribe_members m ON m.user_id = a.user_id AND m.tribe_id = :tribeId AND m.status = 'ACTIVE'
                JOIN activity_definitions d ON d.activity_definition_id = a.activity_definition_id
                JOIN compendium_headings h ON h.heading_code = d.heading_code AND h.counts_for_goals
                JOIN activity_calculations c
                     ON c.activity_id = a.activity_id AND c.is_current AND c.metric_type = :metric
                WHERE a.deleted_at IS NULL AND a.activity_date BETWEEN :start AND :end
                GROUP BY a.user_id, a.activity_date
                """, p);

        Map<UUID, Tally> perMember = new HashMap<>();
        Map<LocalDate, BigDecimal> perDay = new TreeMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) perDay.put(d, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            UUID u = (UUID) r.get("user_id");
            LocalDate day = ((Date) r.get("activity_date")).toLocalDate();
            BigDecimal kcal = (BigDecimal) r.get("kcal");
            Tally t = perMember.computeIfAbsent(u, k -> new Tally());
            t.kcal = t.kcal.add(kcal);
            t.days.add(day);
            t.sessions += ((Number) r.get("sessions")).intValue();
            t.seconds += ((Number) r.get("seconds")).longValue();
            perDay.merge(day, kcal, BigDecimal::add);
            total = total.add(kcal);
        }

        // Leaderboard rows (members with nothing yet included, at 0)
        record Row(Map<String, Object> member, Tally tally) {}
        List<Row> board = new ArrayList<>();
        for (Map<String, Object> m : members) {
            board.add(new Row(m, perMember.getOrDefault((UUID) m.get("user_id"), new Tally())));
        }
        Map<UUID, Integer> rankKcal = new HashMap<>();
        GoalMath.rank(board, r -> r.tally().kcal).forEach(x -> rankKcal.put((UUID) x.item().member().get("user_id"), x.rank()));
        Map<UUID, Integer> rankDays = new HashMap<>();
        GoalMath.rank(board, r -> BigDecimal.valueOf(r.tally().days.size()))
                .forEach(x -> rankDays.put((UUID) x.item().member().get("user_id"), x.rank()));

        // Each member's daily goal (as of today, or the month's last day for a past month)
        // and their target for the month; the tribe's target is the sum of those
        List<UUID> memberIds = members.stream().map(m -> (UUID) m.get("user_id")).toList();
        Map<UUID, List<DailyGoalMath.Change>> goals = dailyGoals.changesFor(memberIds);
        LocalDate goalDay = today.isBefore(start) ? start : today.isAfter(end) ? end : today;
        Map<UUID, Integer> dailyGoalOf = new HashMap<>();
        Map<UUID, Long> monthTargetOf = new HashMap<>();
        long tribeTarget = 0;
        long tribeDaily = 0;
        int membersWithGoal = 0;
        for (UUID u : memberIds) {
            List<DailyGoalMath.Change> c = goals.getOrDefault(u, List.of());
            int daily = DailyGoalMath.goalOn(c, goalDay);
            long memberTarget = DailyGoalMath.target(c, start, end);
            dailyGoalOf.put(u, daily);
            monthTargetOf.put(u, memberTarget);
            tribeTarget += memberTarget;
            tribeDaily += daily;
            if (daily > 0) membersWithGoal++;
        }

        // Progress compares like with like: only calories burned on days a member had a goal
        // count towards the goal (someone who sets a goal on the 24th has 7 days of target,
        // so their 1st-23rd don't count). The goal's own period starts on the first day
        // anyone had one, so pace and projection aren't spread over days without a goal.
        Map<UUID, BigDecimal> goalKcalOf = new HashMap<>();
        BigDecimal goalTotal = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            UUID u = (UUID) r.get("user_id");
            LocalDate day = ((Date) r.get("activity_date")).toLocalDate();
            if (DailyGoalMath.goalOn(goals.getOrDefault(u, List.of()), day) == 0) continue;
            BigDecimal kcal = (BigDecimal) r.get("kcal");
            goalKcalOf.merge(u, kcal, BigDecimal::add);
            goalTotal = goalTotal.add(kcal);
        }
        LocalDate goalStart = null;
        for (LocalDate d = start; !d.isAfter(end) && goalStart == null; d = d.plusDays(1)) {
            for (UUID u : memberIds) {
                if (DailyGoalMath.goalOn(goals.getOrDefault(u, List.of()), d) > 0) {
                    goalStart = d;
                    break;
                }
            }
        }

        BigDecimal totalForShare = total;
        List<Map<String, Object>> leaderboard = new ArrayList<>();
        Map<String, Object> me = null;
        for (Row r : board) {
            UUID u = (UUID) r.member().get("user_id");
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("userId", u);
            e.put("firstName", r.member().get("first_name"));
            e.put("lastName", r.member().get("last_name"));
            e.put("profileId", r.member().get("profile_id"));
            e.put("isMe", u.equals(userId));
            e.put("kcal", r.tally().kcal.setScale(0, RoundingMode.HALF_UP));
            e.put("activeDays", r.tally().days.size());
            e.put("sessions", r.tally().sessions);
            e.put("minutes", r.tally().seconds / 60);
            e.put("rankByKcal", rankKcal.get(u));
            e.put("rankByDays", rankDays.get(u));
            e.put("sharePercent", totalForShare.signum() == 0 ? BigDecimal.ZERO
                    : r.tally().kcal.multiply(BigDecimal.valueOf(100)).divide(totalForShare, 1, RoundingMode.HALF_UP));
            int daily = dailyGoalOf.get(u);
            long monthTarget = monthTargetOf.get(u);
            e.put("dailyGoal", daily == 0 ? null : daily);
            e.put("monthTarget", monthTarget == 0 ? null : monthTarget);
            e.put("goalKcal", goalKcalOf.getOrDefault(u, BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP));
            e.put("goalPercent", monthTarget == 0 ? null
                    : goalKcalOf.getOrDefault(u, BigDecimal.ZERO).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(monthTarget), 1, RoundingMode.HALF_UP));
            leaderboard.add(e);
            if (u.equals(userId)) me = e;
        }
        leaderboard.sort((a, b) -> Integer.compare((Integer) a.get("rankByKcal"), (Integer) b.get("rankByKcal")));

        List<Map<String, Object>> daily = new ArrayList<>();
        perDay.forEach((d, k) -> daily.add(Map.of("date", d, "kcal", k.setScale(0, RoundingMode.HALF_UP))));

        // The tribe's goal: the members' daily goals added up over the month
        Map<String, Object> goal = null;
        Map<String, Object> progress = null;
        if (tribeTarget > 0) {
            goal = new LinkedHashMap<>();
            goal.put("metricType", METRIC);
            goal.put("unit", UNIT);
            goal.put("source", "MEMBERS");
            goal.put("target", tribeTarget);
            goal.put("dailyTarget", tribeDaily);
            goal.put("membersWithGoal", membersWithGoal);
            goal.put("memberCount", members.size());
            goal.put("startsOn", goalStart);
            goal.put("total", goalTotal.setScale(0, RoundingMode.HALF_UP));
            progress = toMap(GoalMath.progress(BigDecimal.valueOf(tribeTarget), goalTotal, goalStart, end, today));
        }

        long totalDays = end.toEpochDay() - start.toEpochDay() + 1;
        long elapsed = today.isBefore(start) ? 0 : today.isAfter(end) ? totalDays : today.toEpochDay() - start.toEpochDay() + 1;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("month", month.toString());
        out.put("periodStart", start);
        out.put("periodEnd", end);
        out.put("today", today);
        out.put("memberCount", members.size());
        out.put("total", total.setScale(0, RoundingMode.HALF_UP));
        out.put("averagePerDay", elapsed == 0 ? null : total.divide(BigDecimal.valueOf(elapsed), 0, RoundingMode.HALF_UP));
        out.put("goal", goal);
        out.put("progress", progress);
        out.put("membersWithGoal", membersWithGoal);
        out.put("daily", daily);
        out.put("leaderboard", leaderboard);
        out.put("me", me);
        return out;
    }

    private static Map<String, Object> toMap(GoalMath.Progress p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("phase", p.phase().name());
        m.put("percent", p.percent());
        m.put("remaining", p.remaining().setScale(0, RoundingMode.HALF_UP));
        m.put("totalDays", p.totalDays());
        m.put("elapsedDays", p.elapsedDays());
        m.put("remainingDays", p.remainingDays());
        m.put("averagePerDay", round(p.averagePerDay()));
        m.put("neededPerDay", round(p.neededPerDay()));
        m.put("projectedTotal", p.projectedTotal());
        m.put("projectedPercent", p.projectedPercent());
        m.put("reached", p.reached());
        return m;
    }

    private static BigDecimal round(BigDecimal v) {
        return v == null ? null : v.setScale(0, RoundingMode.HALF_UP);
    }

    private static final class Tally {
        BigDecimal kcal = BigDecimal.ZERO;
        Set<LocalDate> days = new HashSet<>();
        int sessions;
        long seconds;
    }
}
