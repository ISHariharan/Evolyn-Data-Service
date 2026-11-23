package com.evolyn.evolyn_data_service.Streaks;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evolyn.evolyn_data_service.Common.ApiProblem;
import com.evolyn.evolyn_data_service.Common.EvolynTime;
import com.evolyn.evolyn_data_service.Tribes.TribeService;

/**
 * Personal and tribe streaks, worked out from the activities every time.
 *
 * A person's day counts when they logged at least one activity of the tribe's
 * minimum length (default 20 min), of a type that counts for goals, on that
 * India-time day. "Only me" activities count; deleted ones don't.
 * The tribe's day counts when enough CURRENT members had a counting day
 * (default: half of them, rounded up).
 *
 * Looks back 400 days, which is plenty for "best" and keeps the query bounded.
 */
@Service
public class StreakService {

    static final int DEFAULT_MIN_MINUTES = 20;
    static final int LOOKBACK_DAYS = 400;

    private final TribeService tribes;
    private final NamedParameterJdbcTemplate sql;

    public StreakService(TribeService tribes, NamedParameterJdbcTemplate sql) {
        this.tribes = tribes;
        this.sql = sql;
    }

    // ── Rules ────────────────────────────────────────────────────────────────

    /** Any member can change them (no roles yet). */
    @Transactional
    public Map<String, Object> setRules(UUID tribeId, UUID userId, Integer minMinutes, Integer memberPercent) {
        tribes.requireMember(tribeId, userId, true);
        if (minMinutes == null || minMinutes < 1 || minMinutes > 240) {
            throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Minimum length must be 1 to 240 minutes");
        }
        if (memberPercent == null || memberPercent < 1 || memberPercent > 100) {
            throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Members needed must be 1 to 100%");
        }
        sql.update("""
                UPDATE tribes SET streak_min_minutes = :min, streak_member_percent = :pct, updated_at = now()
                WHERE tribe_id = :tribeId
                """, new MapSqlParameterSource("tribeId", tribeId).addValue("min", minMinutes).addValue("pct", memberPercent));
        return tribeStreaks(tribeId, userId);
    }

    // ── Tribe ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> tribeStreaks(UUID tribeId, UUID userId) {
        tribes.requireMember(tribeId, userId, false);
        LocalDate today = EvolynTime.today();
        MapSqlParameterSource p = new MapSqlParameterSource("tribeId", tribeId);

        Map<String, Object> rules = sql.queryForMap(
                "SELECT streak_min_minutes, streak_member_percent FROM tribes WHERE tribe_id = :tribeId", p);
        int minMinutes = ((Number) rules.get("streak_min_minutes")).intValue();
        int percent = ((Number) rules.get("streak_member_percent")).intValue();

        List<Map<String, Object>> members = sql.queryForList("""
                SELECT u.uuid AS user_id, u.first_name, u.last_name, u.profile_id
                FROM tribe_members m JOIN evolyn_user_details u ON u.uuid = m.user_id
                WHERE m.tribe_id = :tribeId AND m.status = 'ACTIVE'
                ORDER BY m.joined_at
                """, p);

        // Each current member's counting days
        List<Map<String, Object>> rows = sql.queryForList("""
                SELECT DISTINCT a.user_id, a.activity_date
                FROM user_activities a
                JOIN tribe_members m ON m.user_id = a.user_id AND m.tribe_id = :tribeId AND m.status = 'ACTIVE'
                JOIN activity_definitions d ON d.activity_definition_id = a.activity_definition_id
                JOIN compendium_headings h ON h.heading_code = d.heading_code AND h.counts_for_goals
                WHERE a.deleted_at IS NULL AND a.duration_seconds >= :minSeconds
                  AND a.activity_date BETWEEN :from AND :today
                """, p.addValue("minSeconds", minMinutes * 60)
                .addValue("from", Date.valueOf(today.minusDays(LOOKBACK_DAYS)))
                .addValue("today", Date.valueOf(today)));

        Map<UUID, List<LocalDate>> daysByMember = new HashMap<>();
        Map<LocalDate, Integer> activePerDay = new TreeMap<>();
        for (Map<String, Object> r : rows) {
            LocalDate day = ((Date) r.get("activity_date")).toLocalDate();
            daysByMember.computeIfAbsent((UUID) r.get("user_id"), k -> new ArrayList<>()).add(day);
            activePerDay.merge(day, 1, Integer::sum);
        }

        int needed = StreakMath.needed(members.size(), percent);
        Set<LocalDate> tribeDays = StreakMath.tribeDays(activePerDay, needed);
        StreakMath.Streak tribe = StreakMath.of(tribeDays, today);

        List<Map<String, Object>> memberStreaks = new ArrayList<>();
        Map<String, Object> me = null;
        for (Map<String, Object> m : members) {
            UUID u = (UUID) m.get("user_id");
            StreakMath.Streak s = StreakMath.of(daysByMember.getOrDefault(u, List.of()), today);
            Map<String, Object> e = streakMap(s);
            e.put("userId", u);
            e.put("firstName", m.get("first_name"));
            e.put("lastName", m.get("last_name"));
            e.put("profileId", m.get("profile_id"));
            e.put("isMe", u.equals(userId));
            memberStreaks.add(e);
            if (u.equals(userId)) me = e;
        }
        memberStreaks.sort((a, b) -> Integer.compare((Integer) b.get("current"), (Integer) a.get("current")));

        Map<String, Object> tribeOut = streakMap(tribe);
        tribeOut.put("activeMembersToday", activePerDay.getOrDefault(today, 0));
        tribeOut.put("neededMembers", needed);
        tribeOut.put("activeToday", tribeDays.contains(today));

        Map<String, Object> ruleOut = new LinkedHashMap<>();
        ruleOut.put("minMinutes", minMinutes);
        ruleOut.put("memberPercent", percent);
        ruleOut.put("memberCount", members.size());
        ruleOut.put("neededMembers", needed);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("today", today);
        out.put("rules", ruleOut);
        out.put("tribe", tribeOut);
        out.put("members", memberStreaks);
        out.put("me", me);
        // The last 14 days for the tribe: how many members were active, and whether the day counted
        List<Map<String, Object>> recent = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            recent.add(Map.of("date", d, "activeMembers", activePerDay.getOrDefault(d, 0), "counted", tribeDays.contains(d)));
        }
        out.put("recentDays", recent);
        return out;
    }

    // ── Personal (outside any tribe) ─────────────────────────────────────────

    /** Your own streak with the default rule (20+ min, a type that counts), for your Tribe home page. */
    @Transactional(readOnly = true)
    public Map<String, Object> myStreak(UUID userId) {
        if (userId == null) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "userId is required");
        LocalDate today = EvolynTime.today();
        List<LocalDate> days = sql.queryForList("""
                SELECT DISTINCT a.activity_date
                FROM user_activities a
                JOIN activity_definitions d ON d.activity_definition_id = a.activity_definition_id
                JOIN compendium_headings h ON h.heading_code = d.heading_code AND h.counts_for_goals
                WHERE a.user_id = :userId AND a.deleted_at IS NULL AND a.duration_seconds >= :minSeconds
                  AND a.activity_date BETWEEN :from AND :today
                """, new MapSqlParameterSource("userId", userId)
                .addValue("minSeconds", DEFAULT_MIN_MINUTES * 60)
                .addValue("from", Date.valueOf(today.minusDays(LOOKBACK_DAYS)))
                .addValue("today", Date.valueOf(today)), Date.class)
                .stream().map(Date::toLocalDate).collect(Collectors.toList());
        Map<String, Object> out = streakMap(StreakMath.of(days, today));
        out.put("minMinutes", DEFAULT_MIN_MINUTES);
        out.put("today", today);
        return out;
    }

    private static Map<String, Object> streakMap(StreakMath.Streak s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("current", s.current());
        m.put("best", s.best());
        m.put("activeToday", s.activeToday());
        m.put("atRisk", s.atRisk());
        m.put("lastActive", s.lastActive());
        return m;
    }
}
