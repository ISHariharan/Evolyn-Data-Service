package com.evolyn.evolyn_data_service.Tribes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evolyn.evolyn_data_service.Common.ApiProblem;
import com.evolyn.evolyn_data_service.Common.EvolynTime;

/**
 * GET /evolyn/api/internal/tribes/{tribeId}/feed?userId=&before=&limit=
 *
 * The tribe's activity feed: what its current members logged, newest first.
 * Only members can see it, and only activities their owner shared with their
 * tribes (visibility TRIBES), never "only me" ones or deleted ones.
 *
 * Paging: pass the previous page's "nextBefore" as "before" to get older ones.
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal", produces = "application/json")
public class TribeFeedController {

    private static final int MAX_LIMIT = 50;

    private final TribeService tribes;
    private final NamedParameterJdbcTemplate sql;

    public TribeFeedController(TribeService tribes, NamedParameterJdbcTemplate sql) {
        this.tribes = tribes;
        this.sql = sql;
    }

    @GetMapping("/tribes/{tribeId}/feed")
    public Map<String, Object> feed(@PathVariable UUID tribeId, @RequestParam UUID userId,
                                    @RequestParam(required = false) Instant before,
                                    @RequestParam(defaultValue = "20") int limit) {
        tribes.requireMember(tribeId, userId, false);
        if (limit < 1 || limit > MAX_LIMIT) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "limit must be 1 to " + MAX_LIMIT);

        MapSqlParameterSource p = new MapSqlParameterSource("tribeId", tribeId)
                .addValue("me", userId)
                .addValue("before", Timestamp.from(before != null ? before : Instant.now().plusSeconds(60)))
                .addValue("limit", limit);

        List<Map<String, Object>> items = sql.query("""
                SELECT a.activity_id, a.user_id, u.first_name, u.last_name, u.profile_id,
                       d.activity_code, d.description, d.heading_code, h.counts_for_goals,
                       a.started_at, a.activity_date, a.duration_seconds, a.distance_km,
                       c.metric_value AS kcal, c.formula_version
                FROM user_activities a
                JOIN tribe_members m ON m.user_id = a.user_id AND m.tribe_id = :tribeId AND m.status = 'ACTIVE'
                JOIN evolyn_user_details u ON u.uuid = a.user_id
                JOIN activity_definitions d ON d.activity_definition_id = a.activity_definition_id
                JOIN compendium_headings h ON h.heading_code = d.heading_code
                LEFT JOIN activity_calculations c
                       ON c.activity_id = a.activity_id AND c.is_current AND c.metric_type = 'CALORIES_BURNED'
                WHERE a.deleted_at IS NULL AND a.visibility = 'TRIBES' AND a.started_at < :before
                ORDER BY a.started_at DESC, a.activity_id
                LIMIT :limit
                """, p, (rs, i) -> {
            Instant started = rs.getTimestamp("started_at").toInstant();
            UUID owner = rs.getObject("user_id", UUID.class);
            BigDecimal kcal = rs.getBigDecimal("kcal");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("activityId", rs.getObject("activity_id", UUID.class));
            item.put("userId", owner);
            item.put("firstName", rs.getString("first_name"));
            item.put("lastName", rs.getString("last_name"));
            item.put("profileId", rs.getString("profile_id"));
            item.put("isMe", owner.equals(userId));
            item.put("activityCode", rs.getString("activity_code"));
            item.put("description", rs.getString("description"));
            item.put("headingCode", rs.getString("heading_code"));
            item.put("countsForGoals", rs.getBoolean("counts_for_goals"));
            item.put("startedAt", LocalDateTime.ofInstant(started, EvolynTime.ZONE)); // India time
            item.put("startedAtInstant", started);
            item.put("activityDate", rs.getDate("activity_date").toLocalDate());
            item.put("durationMinutes", rs.getInt("duration_seconds") / 60);
            item.put("distanceKm", rs.getBigDecimal("distance_km"));
            item.put("kcal", kcal == null ? null : kcal.setScale(0, RoundingMode.HALF_UP));
            // Entered by the person (e.g. from a watch) rather than estimated
            item.put("kcalEntered", "ENTERED_V1".equals(rs.getString("formula_version")));
            return item;
        });

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        // More may be older than the last one shown
        out.put("nextBefore", items.size() == limit ? items.get(items.size() - 1).get("startedAtInstant") : null);
        return out;
    }
}
