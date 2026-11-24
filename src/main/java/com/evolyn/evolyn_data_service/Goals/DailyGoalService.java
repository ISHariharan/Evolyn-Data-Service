package com.evolyn.evolyn_data_service.Goals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evolyn.evolyn_data_service.Common.ApiProblem;
import com.evolyn.evolyn_data_service.Common.EvolynTime;

/**
 * Your daily calorie goal: read it, set it, and see today's progress.
 * A new goal applies from today (India date); earlier days keep the goal they had.
 */
@Service
public class DailyGoalService {

    public static final int MIN_KCAL = 50;
    public static final int MAX_KCAL = 5000;

    private final NamedParameterJdbcTemplate sql;

    public DailyGoalService(NamedParameterJdbcTemplate sql) {
        this.sql = sql;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> mine(UUID userId) {
        LocalDate today = EvolynTime.today();
        YearMonth month = YearMonth.from(today);
        List<DailyGoalMath.Change> changes = changesFor(List.of(userId)).getOrDefault(userId, List.of());
        int goal = DailyGoalMath.goalOn(changes, today);

        BigDecimal todayKcal = countedKcal(userId, today, today);
        // Only days you had a goal count towards your month's target
        LocalDate firstGoalDay = changes.stream().map(DailyGoalMath.Change::effectiveFrom)
                .min(LocalDate::compareTo).orElse(null);
        LocalDate countFrom = firstGoalDay == null || firstGoalDay.isBefore(month.atDay(1)) ? month.atDay(1) : firstGoalDay;
        BigDecimal monthKcal = firstGoalDay == null || countFrom.isAfter(month.atEndOfMonth()) ? BigDecimal.ZERO
                : countedKcal(userId, countFrom, month.atEndOfMonth());
        long monthTarget = DailyGoalMath.target(changes, month.atDay(1), month.atEndOfMonth());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("today", today);
        out.put("kcal", goal == 0 ? null : goal);
        out.put("effectiveFrom", changes.stream().map(DailyGoalMath.Change::effectiveFrom)
                .filter(d -> !d.isAfter(today)).max(LocalDate::compareTo).orElse(null));
        out.put("todayKcal", todayKcal.setScale(0, RoundingMode.HALF_UP));
        out.put("todayPercent", goal == 0 ? null
                : todayKcal.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(goal), 1, RoundingMode.HALF_UP));
        out.put("month", month.toString());
        out.put("monthKcal", monthKcal.setScale(0, RoundingMode.HALF_UP));
        out.put("monthTarget", monthTarget == 0 ? null : monthTarget);
        out.put("min", MIN_KCAL);
        out.put("max", MAX_KCAL);
        return out;
    }

    @Transactional
    public Map<String, Object> set(UUID userId, Integer kcal) {
        if (kcal == null || kcal < MIN_KCAL || kcal > MAX_KCAL) {
            throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Daily goal must be between " + MIN_KCAL + " and " + MAX_KCAL + " kcal");
        }
        // Same day again: correct today's row instead of adding another
        sql.update("""
                INSERT INTO user_daily_goals (user_id, effective_from, kcal, set_at)
                VALUES (:userId, :today, :kcal, :now)
                ON CONFLICT (user_id, effective_from) DO UPDATE SET kcal = EXCLUDED.kcal, set_at = EXCLUDED.set_at
                """, new MapSqlParameterSource("userId", userId)
                .addValue("today", Date.valueOf(EvolynTime.today()))
                .addValue("kcal", kcal)
                .addValue("now", Timestamp.from(Instant.now())));
        return mine(userId);
    }

    /** Everyone's goal history, oldest first, for the given people. */
    public Map<UUID, List<DailyGoalMath.Change>> changesFor(Collection<UUID> userIds) {
        Map<UUID, List<DailyGoalMath.Change>> out = new HashMap<>();
        if (userIds.isEmpty()) return out;
        sql.query("""
                SELECT user_id, effective_from, kcal FROM user_daily_goals
                WHERE user_id IN (:ids) ORDER BY effective_from
                """, new MapSqlParameterSource("ids", userIds), rs -> {
            out.computeIfAbsent((UUID) rs.getObject("user_id"), k -> new java.util.ArrayList<>())
                    .add(new DailyGoalMath.Change(rs.getDate("effective_from").toLocalDate(), rs.getInt("kcal")));
        });
        return out;
    }

    // Calories that count for goals (same rules as the tribe dashboard)
    private BigDecimal countedKcal(UUID userId, LocalDate from, LocalDate to) {
        BigDecimal v = sql.queryForObject("""
                SELECT coalesce(sum(c.metric_value), 0)
                FROM user_activities a
                JOIN activity_definitions d ON d.activity_definition_id = a.activity_definition_id
                JOIN compendium_headings h ON h.heading_code = d.heading_code AND h.counts_for_goals
                JOIN activity_calculations c
                     ON c.activity_id = a.activity_id AND c.is_current AND c.metric_type = 'CALORIES_BURNED'
                WHERE a.user_id = :userId AND a.deleted_at IS NULL AND a.activity_date BETWEEN :from AND :to
                """, new MapSqlParameterSource("userId", userId)
                .addValue("from", Date.valueOf(from)).addValue("to", Date.valueOf(to)), BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }
}
