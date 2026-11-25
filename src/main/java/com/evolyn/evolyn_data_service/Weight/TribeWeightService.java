package com.evolyn.evolyn_data_service.Weight;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evolyn.evolyn_data_service.Common.EvolynTime;
import com.evolyn.evolyn_data_service.Tribes.TribeService;

/**
 * Weight progress inside a tribe, and the setting that allows it.
 *
 * Privacy rules (weight is private by default, see V2 and V13):
 *   - Nobody's weight is shown unless they turned sharing on.
 *   - Even then, other members only get the CHANGE in kg (start = 0), never the weight
 *     and never a percentage (kg change + percent together would reveal the weight).
 *   - You always see your own numbers, including your kg, shared or not.
 */
@Service
public class TribeWeightService {

    private final TribeService tribes;
    private final NamedParameterJdbcTemplate sql;

    public TribeWeightService(TribeService tribes, NamedParameterJdbcTemplate sql) {
        this.tribes = tribes;
        this.sql = sql;
    }

    // ── The setting ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> sharing(UUID userId) {
        List<Boolean> v = sql.queryForList(
                "SELECT share_weight_change FROM user_privacy_settings WHERE user_id = :u",
                new MapSqlParameterSource("u", userId), Boolean.class);
        return Map.of("shareWeightChange", !v.isEmpty() && Boolean.TRUE.equals(v.get(0)));
    }

    @Transactional
    public Map<String, Object> setSharing(UUID userId, boolean share) {
        sql.update("""
                INSERT INTO user_privacy_settings (user_id, share_weight_change, updated_at)
                VALUES (:u, :share, :now)
                ON CONFLICT (user_id) DO UPDATE SET share_weight_change = EXCLUDED.share_weight_change,
                                                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource("u", userId).addValue("share", share)
                .addValue("now", Timestamp.from(Instant.now())));
        return sharing(userId);
    }

    // ── The tribe's month ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> progress(UUID tribeId, UUID userId, YearMonth month) {
        tribes.requireMember(tribeId, userId, false);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        MapSqlParameterSource p = new MapSqlParameterSource("tribeId", tribeId)
                .addValue("start", Date.valueOf(start)).addValue("end", Date.valueOf(end));

        List<Map<String, Object>> members = sql.queryForList("""
                SELECT u.uuid AS user_id, u.first_name, u.last_name, u.profile_id,
                       coalesce(s.share_weight_change, false) AS shares
                FROM tribe_members m
                JOIN evolyn_user_details u ON u.uuid = m.user_id
                LEFT JOIN user_privacy_settings s ON s.user_id = m.user_id
                WHERE m.tribe_id = :tribeId AND m.status = 'ACTIVE'
                ORDER BY m.joined_at
                """, p);

        // Weigh-ins only for people whose numbers may be shown: sharers and you
        Map<UUID, List<WeightProgressMath.WeighIn>> weighIns = new HashMap<>();
        sql.query("""
                SELECT w.user_id, w.recorded_on, w.weight_kg
                FROM user_weight_logs w
                JOIN tribe_members m ON m.user_id = w.user_id AND m.tribe_id = :tribeId AND m.status = 'ACTIVE'
                LEFT JOIN user_privacy_settings s ON s.user_id = w.user_id
                WHERE w.recorded_on BETWEEN :start AND :end
                  AND (coalesce(s.share_weight_change, false) OR w.user_id = :me)
                """, p.addValue("me", userId), rs -> {
            weighIns.computeIfAbsent((UUID) rs.getObject("user_id"), k -> new ArrayList<>())
                    .add(new WeightProgressMath.WeighIn(rs.getDate("recorded_on").toLocalDate(), rs.getBigDecimal("weight_kg")));
        });

        List<Map<String, Object>> people = new ArrayList<>();
        boolean iShare = false;
        for (Map<String, Object> m : members) {
            UUID u = (UUID) m.get("user_id");
            boolean isMe = u.equals(userId);
            boolean shares = Boolean.TRUE.equals(m.get("shares"));
            if (isMe) iShare = shares;

            Map<String, Object> e = new LinkedHashMap<>();
            e.put("userId", u);
            e.put("firstName", m.get("first_name"));
            e.put("lastName", m.get("last_name"));
            e.put("profileId", m.get("profile_id"));
            e.put("isMe", isMe);
            e.put("shared", shares);
            if (shares || isMe) {
                WeightProgressMath.Progress pr = WeightProgressMath.progress(weighIns.getOrDefault(u, List.of()));
                e.put("weighIns", pr == null ? 0 : pr.weighIns());
                e.put("startDate", pr == null ? null : pr.startDate());
                e.put("latestDate", pr == null ? null : pr.latestDate());
                e.put("changeKg", pr == null ? null : pr.changeKg());
                e.put("points", pr == null ? List.of() : pr.points().stream()
                        .map(pt -> Map.of("date", pt.date(), "changeKg", pt.changeKg())).toList());
                if (isMe && pr != null) {
                    // Your own weights, for you only
                    e.put("startKg", pr.startKg());
                    e.put("latestKg", pr.latestKg());
                }
            }
            people.add(e);
        }
        // Kept in joining order: the website gives each person a stable chart colour from it

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("month", month.toString());
        out.put("periodStart", start);
        out.put("periodEnd", end);
        out.put("today", EvolynTime.today());
        out.put("memberCount", members.size());
        out.put("sharingCount", members.stream().filter(m -> Boolean.TRUE.equals(m.get("shares"))).count());
        out.put("iShare", iShare);
        out.put("members", people);
        return out;
    }
}
