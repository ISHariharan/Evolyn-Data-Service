package com.evolyn.evolyn_data_service.Goals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Instant;
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
 * Any member can set the goal (no roles yet). Past months are read-only.
 */
@Service
public class TribeDashboardService {

    static final String METRIC = "CALORIES_BURNED";
    static final String UNIT = "kcal";
    static final BigDecimal MAX_TARGET = new BigDecimal("10000000");

    private final TribeService tribes;
    private final NamedParameterJdbcTemplate sql;

    public TribeDashboardService(TribeService tribes, NamedParameterJdbcTemplate sql) {
        this.tribes = tribes;
        this.sql = sql;
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

    // ── Setting the goal ─────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> setGoal(UUID tribeId, UUID userId, YearMonth month, BigDecimal target) {
        tribes.requireMember(tribeId, userId, true);
        if (month.isBefore(YearMonth.from(EvolynTime.today()))) {
            throw ApiProblem.of(HttpStatus.CONFLICT, "Past months can't be changed");
        }
        if (target == null || target.signum() <= 0 || target.compareTo(MAX_TARGET) > 0) {
            throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Target must be between 1 and 10,000,000 kcal");
        }
        BigDecimal t = target.setScale(0, RoundingMode.HALF_UP);
        MapSqlParameterSource p = new MapSqlParameterSource("tribeId", tribeId)
                .addValue("metric", METRIC)
                .addValue("start", Date.valueOf(month.atDay(1)))
                .addValue("end", Date.valueOf(month.atEndOfMonth()))
                .addValue("target", t)
                .addValue("userId", userId)
                .addValue("now", java.sql.Timestamp.from(Instant.now()));

        List<Map<String, Object>> existing = sql.queryForList(
                "SELECT goal_id, target_value FROM tribe_goals WHERE tribe_id = :tribeId AND metric_type = :metric AND period_start = :start",
                p);
        UUID goalId;
        BigDecimal old = null;
        if (existing.isEmpty()) {
            goalId = UUID.randomUUID();
            sql.update("""
                    INSERT INTO tribe_goals (goal_id, tribe_id, metric_type, unit, target_value, period_type,
                                             period_start, period_end, created_by, created_at, updated_by, updated_at)
                    VALUES (:goalId, :tribeId, :metric, :unit, :target, 'MONTHLY', :start, :end, :userId, :now, :userId, :now)
                    """, p.addValue("goalId", goalId).addValue("unit", UNIT));
        } else {
            goalId = (UUID) existing.get(0).get("goal_id");
            old = (BigDecimal) existing.get(0).get("target_value");
            if (old.compareTo(t) == 0) return dashboard(tribeId, userId, month); // unchanged
            sql.update("UPDATE tribe_goals SET target_value = :target, updated_by = :userId, updated_at = :now WHERE goal_id = :goalId",
                    p.addValue("goalId", goalId));
        }
        sql.update("""
                INSERT INTO tribe_goal_changes (change_id, goal_id, old_target, new_target, changed_by, changed_at)
                VALUES (:changeId, :goalId, :old, :target, :userId, :now)
                """, p.addValue("changeId", UUID.randomUUID()).addValue("goalId", goalId).addValue("old", old));
        return dashboard(tribeId, userId, month);
    }

    /** Who set or changed this month's goal, newest first. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> goalHistory(UUID tribeId, UUID userId, YearMonth month) {
        tribes.requireMember(tribeId, userId, false);
        return sql.queryForList("""
                SELECT ch.old_target AS "oldTarget", ch.new_target AS "newTarget", ch.changed_at AS "changedAt",
                       u.first_name AS "firstName", u.profile_id AS "profileId"
                FROM tribe_goal_changes ch
                JOIN tribe_goals g ON g.goal_id = ch.goal_id
                JOIN evolyn_user_details u ON u.uuid = ch.changed_by
                WHERE g.tribe_id = :tribeId AND g.metric_type = :metric AND g.period_start = :start
                ORDER BY ch.changed_at DESC
                """, new MapSqlParameterSource("tribeId", tribeId).addValue("metric", METRIC)
                .addValue("start", Date.valueOf(month.atDay(1))));
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
            leaderboard.add(e);
            if (u.equals(userId)) me = e;
        }
        leaderboard.sort((a, b) -> Integer.compare((Integer) a.get("rankByKcal"), (Integer) b.get("rankByKcal")));

        List<Map<String, Object>> daily = new ArrayList<>();
        perDay.forEach((d, k) -> daily.add(Map.of("date", d, "kcal", k.setScale(0, RoundingMode.HALF_UP))));

        // This month's goal, or a suggestion from the latest earlier one
        List<Map<String, Object>> goalRows = sql.queryForList("""
                SELECT g.goal_id, g.target_value, g.updated_at, u.first_name AS updated_by_name, u.profile_id AS updated_by_profile
                FROM tribe_goals g JOIN evolyn_user_details u ON u.uuid = g.updated_by
                WHERE g.tribe_id = :tribeId AND g.metric_type = :metric AND g.period_start = :start
                """, p);
        Map<String, Object> goal = null;
        Map<String, Object> progress = null;
        if (!goalRows.isEmpty()) {
            Map<String, Object> g = goalRows.get(0);
            BigDecimal target = (BigDecimal) g.get("target_value");
            goal = new LinkedHashMap<>();
            goal.put("goalId", g.get("goal_id"));
            goal.put("metricType", METRIC);
            goal.put("unit", UNIT);
            goal.put("target", target.setScale(0, RoundingMode.HALF_UP));
            goal.put("updatedAt", g.get("updated_at"));
            goal.put("updatedByName", g.get("updated_by_name"));
            goal.put("updatedByProfileId", g.get("updated_by_profile"));
            progress = toMap(GoalMath.progress(target, total, start, end, today));
        }
        List<BigDecimal> earlier = sql.queryForList("""
                SELECT target_value FROM tribe_goals
                WHERE tribe_id = :tribeId AND metric_type = :metric AND period_start < :start
                ORDER BY period_start DESC LIMIT 1
                """, p, BigDecimal.class);

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
        out.put("suggestedTarget", earlier.isEmpty() ? null : earlier.get(0).setScale(0, RoundingMode.HALF_UP));
        out.put("canSetGoal", !month.isBefore(YearMonth.from(today)));
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
