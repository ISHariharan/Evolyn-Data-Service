package com.evolyn.evolyn_data_service.Challenges;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

import com.evolyn.evolyn_data_service.Challenges.ChallengeMath.Phase;
import com.evolyn.evolyn_data_service.Challenges.ChallengeMath.Type;
import com.evolyn.evolyn_data_service.Common.ApiProblem;
import com.evolyn.evolyn_data_service.Common.EvolynTime;
import com.evolyn.evolyn_data_service.Tribes.TribeService;

/**
 * Challenges inside a tribe: create, publish, join, standings and frozen results.
 *
 * Rules:
 *  - Any member can create, publish, edit or cancel (no roles yet). Drafts are
 *    seen only by whoever created them.
 *  - Joining is open while upcoming and on the first day; leaving only before it starts.
 *  - Before the start everything can be edited; once it starts only the name
 *    and description, so nobody changes the rules mid-game.
 *  - What counts: a participant's activities dated in the challenge (India time),
 *    at least min_minutes long, of a type that counts for goals (and of the
 *    chosen heading, if one is set). "Only me" activities count; deleted don't.
 *  - The day after it ends is "under review" (late logging of in-period
 *    activities is fine); after that the results are frozen for good.
 */
@Service
public class ChallengeService {

    static final int MAX_DAYS = 90;
    static final BigDecimal MAX_KCAL = new BigDecimal("10000000");

    private final TribeService tribes;
    private final NamedParameterJdbcTemplate sql;

    public ChallengeService(TribeService tribes, NamedParameterJdbcTemplate sql) {
        this.tribes = tribes;
        this.sql = sql;
    }

    /** What the browser sends to create or edit. */
    public record ChallengeRequest(String name, String description, String type, LocalDate startDate, LocalDate endDate,
                                   BigDecimal target, Integer minMinutes, String headingCode, Boolean publish) {}

    // ── List ─────────────────────────────────────────────────────────────────

    @Transactional
    public List<Map<String, Object>> list(UUID tribeId, UUID userId) {
        tribes.requireMember(tribeId, userId, false);
        List<UUID> ids = sql.queryForList("""
                SELECT challenge_id FROM challenges
                WHERE tribe_id = :tribeId AND (status <> 'DRAFT' OR created_by = :userId)
                ORDER BY start_date DESC, created_at DESC
                """, new MapSqlParameterSource("tribeId", tribeId).addValue("userId", userId), UUID.class);
        List<Map<String, Object>> out = new ArrayList<>();
        for (UUID id : ids) out.add(summary(detailOf(id, userId)));
        return out;
    }

    private static Map<String, Object> summary(Map<String, Object> d) {
        Map<String, Object> s = new LinkedHashMap<>();
        for (String k : List.of("challengeId", "name", "type", "unit", "target", "startDate", "endDate", "phase",
                "participantCount", "joined", "myTeam", "days", "total", "totalPercent", "completed", "teams", "isCreator")) {
            s.put(k, d.get(k));
        }
        s.put("me", d.get("me"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> standings = (List<Map<String, Object>>) d.get("standings");
        s.put("leader", standings.isEmpty() ? null : standings.get(0));
        return s;
    }

    // ── Create / edit / publish / cancel ─────────────────────────────────────

    @Transactional
    public Map<String, Object> create(UUID tribeId, UUID userId, ChallengeRequest r) {
        tribes.requireMember(tribeId, userId, true);
        Checked c = check(r, true);
        UUID id = UUID.randomUUID();
        boolean publish = Boolean.TRUE.equals(r.publish());
        sql.update("""
                INSERT INTO challenges (challenge_id, tribe_id, created_by, name, description, challenge_type, metric_type,
                    target_value, start_date, end_date, min_minutes, heading_code, status, published_at, created_at, updated_at)
                VALUES (:id, :tribeId, :userId, :name, :description, :type, :metric, :target, :start, :end, :min,
                    :heading, :status, :publishedAt, now(), now())
                """, c.params().addValue("id", id).addValue("tribeId", tribeId).addValue("userId", userId)
                .addValue("status", publish ? "PUBLISHED" : "DRAFT")
                .addValue("publishedAt", publish ? Timestamp.from(Instant.now()) : null));
        return detailOf(id, userId);
    }

    @Transactional
    public Map<String, Object> update(UUID challengeId, UUID userId, ChallengeRequest r) {
        Map<String, Object> ch = load(challengeId, userId);
        Phase phase = phaseOf(ch);
        if (phase == Phase.COMPLETED || phase == Phase.CANCELLED) {
            throw ApiProblem.of(HttpStatus.CONFLICT, "This challenge is over and can't be changed");
        }
        if (phase == Phase.DRAFT || phase == Phase.UPCOMING) {
            Checked c = check(r, true);
            sql.update("""
                    UPDATE challenges SET name = :name, description = :description, challenge_type = :type,
                        metric_type = :metric, target_value = :target, start_date = :start, end_date = :end,
                        min_minutes = :min, heading_code = :heading, updated_at = now()
                    WHERE challenge_id = :id
                    """, c.params().addValue("id", challengeId));
        } else {
            // Running: only the name and description, never the rules
            String name = cleanName(r.name());
            sql.update("UPDATE challenges SET name = :name, description = :description, updated_at = now() WHERE challenge_id = :id",
                    new MapSqlParameterSource("id", challengeId).addValue("name", name)
                            .addValue("description", cleanDescription(r.description())));
        }
        return detailOf(challengeId, userId);
    }

    @Transactional
    public Map<String, Object> publish(UUID challengeId, UUID userId) {
        Map<String, Object> ch = load(challengeId, userId);
        if (phaseOf(ch) != Phase.DRAFT) throw ApiProblem.of(HttpStatus.CONFLICT, "Only a draft can be published");
        if (toDate(ch.get("start_date")).isBefore(EvolynTime.today())) {
            throw ApiProblem.of(HttpStatus.CONFLICT, "The start date has passed. Pick new dates first.");
        }
        sql.update("UPDATE challenges SET status = 'PUBLISHED', published_at = now(), updated_at = now() WHERE challenge_id = :id",
                new MapSqlParameterSource("id", challengeId));
        return detailOf(challengeId, userId);
    }

    @Transactional
    public Map<String, Object> cancel(UUID challengeId, UUID userId) {
        Map<String, Object> ch = load(challengeId, userId);
        Phase phase = phaseOf(ch);
        if (phase == Phase.COMPLETED || phase == Phase.CANCELLED) {
            throw ApiProblem.of(HttpStatus.CONFLICT, "This challenge is already over");
        }
        sql.update("UPDATE challenges SET status = 'CANCELLED', cancelled_at = now(), updated_at = now() WHERE challenge_id = :id",
                new MapSqlParameterSource("id", challengeId));
        return detailOf(challengeId, userId);
    }

    // ── Join / leave ─────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> join(UUID challengeId, UUID userId, String team) {
        Map<String, Object> ch = load(challengeId, userId);
        Phase phase = phaseOf(ch);
        LocalDate start = toDate(ch.get("start_date"));
        boolean firstDay = phase == Phase.ACTIVE && EvolynTime.today().equals(start);
        if (phase != Phase.UPCOMING && !firstDay) {
            throw ApiProblem.of(HttpStatus.CONFLICT, phase == Phase.DRAFT
                    ? "This challenge isn't published yet" : "Joining closed at the end of the first day");
        }
        boolean isTeam = Type.TEAM.name().equals(ch.get("challenge_type"));
        String t = team == null ? null : team.trim().toUpperCase();
        if (isTeam && !"A".equals(t) && !"B".equals(t)) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Pick team A or team B");
        if (!isTeam) t = null;

        MapSqlParameterSource p = new MapSqlParameterSource("id", challengeId).addValue("userId", userId).addValue("team", t);
        int updated = sql.update("""
                UPDATE challenge_participants SET status = 'JOINED', team = :team, joined_at = now(), left_at = NULL
                WHERE challenge_id = :id AND user_id = :userId
                """, p);
        if (updated == 0) {
            sql.update("""
                    INSERT INTO challenge_participants (participant_id, challenge_id, user_id, team, status, joined_at)
                    VALUES (:pid, :id, :userId, :team, 'JOINED', now())
                    """, p.addValue("pid", UUID.randomUUID()));
        }
        return detailOf(challengeId, userId);
    }

    @Transactional
    public Map<String, Object> leave(UUID challengeId, UUID userId) {
        Map<String, Object> ch = load(challengeId, userId);
        if (phaseOf(ch) != Phase.UPCOMING) throw ApiProblem.of(HttpStatus.CONFLICT, "You can only leave before it starts");
        sql.update("""
                UPDATE challenge_participants SET status = 'LEFT', left_at = now()
                WHERE challenge_id = :id AND user_id = :userId AND status = 'JOINED'
                """, new MapSqlParameterSource("id", challengeId).addValue("userId", userId));
        return detailOf(challengeId, userId);
    }

    // ── Detail (standings, your activities, what you can do) ─────────────────

    @Transactional
    public Map<String, Object> detail(UUID challengeId, UUID userId) {
        load(challengeId, userId);
        return detailOf(challengeId, userId);
    }

    private Map<String, Object> detailOf(UUID challengeId, UUID userId) {
        Map<String, Object> ch = sql.queryForMap("""
                SELECT c.*, u.first_name AS creator_first, u.profile_id AS creator_profile, h.name AS heading_name
                FROM challenges c
                JOIN evolyn_user_details u ON u.uuid = c.created_by
                LEFT JOIN compendium_headings h ON h.heading_code = c.heading_code
                WHERE c.challenge_id = :id
                """, new MapSqlParameterSource("id", challengeId));
        Type type = Type.valueOf((String) ch.get("challenge_type"));
        LocalDate start = toDate(ch.get("start_date"));
        LocalDate end = toDate(ch.get("end_date"));
        LocalDate today = EvolynTime.today();
        BigDecimal target = (BigDecimal) ch.get("target_value");
        Phase phase = phaseOf(ch);

        // Freeze the results the first time it's looked at after the review day
        if (phase == Phase.COMPLETED && ch.get("finalized_at") == null) {
            finalizeResults(challengeId, type, target, start, end, ch);
            ch.put("finalized_at", sql.queryForObject("SELECT finalized_at FROM challenges WHERE challenge_id = :id",
                    new MapSqlParameterSource("id", challengeId), Timestamp.class));
        }

        Map<UUID, Map<String, Object>> names = new HashMap<>();
        for (Map<String, Object> r : sql.queryForList("""
                SELECT p.user_id, p.team, u.first_name, u.last_name, u.profile_id
                FROM challenge_participants p JOIN evolyn_user_details u ON u.uuid = p.user_id
                WHERE p.challenge_id = :id AND p.status = 'JOINED'
                """, new MapSqlParameterSource("id", challengeId))) {
            names.put((UUID) r.get("user_id"), r);
        }

        ChallengeMath.Standings s = phase == Phase.COMPLETED
                ? frozenStandings(challengeId, type, target)
                : ChallengeMath.score(type, target, tallies(challengeId, start, end, ch));

        List<Map<String, Object>> standings = new ArrayList<>();
        Map<String, Object> me = null;
        for (ChallengeMath.PersonResult p : s.people()) {
            Map<String, Object> n = names.get(p.userId());
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("userId", p.userId());
            e.put("firstName", n == null ? "" : n.get("first_name"));
            e.put("lastName", n == null ? "" : n.get("last_name"));
            e.put("profileId", n == null ? null : n.get("profile_id"));
            e.put("isMe", p.userId().equals(userId));
            e.put("team", p.team());
            e.put("value", p.value().setScale(0, RoundingMode.HALF_UP));
            e.put("percent", p.percent());
            e.put("completed", p.completed());
            e.put("rank", p.rank());
            standings.add(e);
            if (p.userId().equals(userId)) me = e;
        }
        List<Map<String, Object>> teams = new ArrayList<>();
        for (ChallengeMath.TeamResult t : s.teams()) {
            teams.add(Map.of("team", t.team(), "value", t.value().setScale(0, RoundingMode.HALF_UP),
                    "members", t.members(), "rank", t.rank(), "won", t.won()));
        }

        boolean joined = names.containsKey(userId);
        boolean isCreator = userId.equals(ch.get("created_by"));
        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        long elapsed = today.isBefore(start) ? 0 : today.isAfter(end) ? totalDays : ChronoUnit.DAYS.between(start, today) + 1;
        boolean firstDay = phase == Phase.ACTIVE && today.equals(start);
        boolean running = phase == Phase.ACTIVE || phase == Phase.UNDER_REVIEW;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("challengeId", challengeId);
        out.put("tribeId", ch.get("tribe_id"));
        out.put("name", ch.get("name"));
        out.put("description", ch.get("description"));
        out.put("type", type.name());
        out.put("unit", type == Type.CONSISTENCY ? "days" : "kcal");
        out.put("target", target == null ? null : target.setScale(0, RoundingMode.HALF_UP));
        out.put("startDate", start);
        out.put("endDate", end);
        out.put("minMinutes", ch.get("min_minutes"));
        out.put("headingCode", ch.get("heading_code"));
        out.put("headingName", ch.get("heading_name"));
        out.put("status", ch.get("status"));
        out.put("phase", phase.name());
        out.put("createdByName", ch.get("creator_first"));
        out.put("createdByProfileId", ch.get("creator_profile"));
        out.put("isCreator", isCreator);
        out.put("finalizedAt", ch.get("finalized_at"));
        out.put("today", today);
        out.put("days", Map.of("total", totalDays, "elapsed", elapsed, "remaining", totalDays - elapsed));
        out.put("participantCount", names.size());
        out.put("joined", joined);
        out.put("myTeam", joined ? names.get(userId).get("team") : null);
        out.put("standings", standings);
        out.put("teams", teams);
        out.put("total", s.total().setScale(0, RoundingMode.HALF_UP));
        out.put("totalPercent", s.totalPercent());
        out.put("completed", s.completed());
        out.put("me", me);
        out.put("myActivities", joined && phase != Phase.UPCOMING && phase != Phase.DRAFT
                ? myActivities(userId, start, end, ch) : List.of());
        out.put("canJoin", !joined && (phase == Phase.UPCOMING || firstDay));
        out.put("canLeave", joined && phase == Phase.UPCOMING);
        out.put("canEditRules", phase == Phase.DRAFT || phase == Phase.UPCOMING);
        out.put("canEditName", phase == Phase.DRAFT || phase == Phase.UPCOMING || running);
        out.put("canPublish", phase == Phase.DRAFT);
        out.put("canCancel", phase != Phase.COMPLETED && phase != Phase.CANCELLED);
        return out;
    }

    /** Each participant's kcal and counting days in the period. */
    private List<ChallengeMath.Tally> tallies(UUID challengeId, LocalDate start, LocalDate end, Map<String, Object> ch) {
        List<ChallengeMath.Tally> out = new ArrayList<>();
        for (Map<String, Object> r : sql.queryForList("""
                SELECT p.user_id, p.team,
                       coalesce(sum(c.metric_value), 0) AS kcal,
                       count(DISTINCT a.activity_date) AS days
                FROM challenge_participants p
                LEFT JOIN (user_activities a
                           JOIN activity_definitions d ON d.activity_definition_id = a.activity_definition_id
                           JOIN compendium_headings h ON h.heading_code = d.heading_code AND h.counts_for_goals)
                       ON a.user_id = p.user_id AND a.deleted_at IS NULL
                      AND a.activity_date BETWEEN :start AND :end
                      AND a.duration_seconds >= :minSeconds
                      AND (CAST(:heading AS varchar) IS NULL OR d.heading_code = CAST(:heading AS varchar))
                LEFT JOIN activity_calculations c
                       ON c.activity_id = a.activity_id AND c.is_current AND c.metric_type = 'CALORIES_BURNED'
                WHERE p.challenge_id = :id AND p.status = 'JOINED'
                GROUP BY p.user_id, p.team
                """, rules(challengeId, start, end, ch))) {
            out.add(new ChallengeMath.Tally((UUID) r.get("user_id"), (String) r.get("team"),
                    (BigDecimal) r.get("kcal"), ((Number) r.get("days")).intValue()));
        }
        return out;
    }

    /** Your activities in the period, and why any of them didn't count. */
    private List<Map<String, Object>> myActivities(UUID userId, LocalDate start, LocalDate end, Map<String, Object> ch) {
        int min = ((Number) ch.get("min_minutes")).intValue();
        String heading = (String) ch.get("heading_code");
        String headingName = (String) ch.get("heading_name");
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : sql.queryForList("""
                SELECT a.activity_id, d.description, d.heading_code, h.counts_for_goals, a.started_at, a.activity_date,
                       a.duration_seconds, c.metric_value AS kcal
                FROM user_activities a
                JOIN activity_definitions d ON d.activity_definition_id = a.activity_definition_id
                JOIN compendium_headings h ON h.heading_code = d.heading_code
                LEFT JOIN activity_calculations c
                       ON c.activity_id = a.activity_id AND c.is_current AND c.metric_type = 'CALORIES_BURNED'
                WHERE a.user_id = :userId AND a.deleted_at IS NULL AND a.activity_date BETWEEN :start AND :end
                ORDER BY a.started_at DESC
                """, new MapSqlParameterSource("userId", userId).addValue("start", Date.valueOf(start)).addValue("end", Date.valueOf(end)))) {
            int minutes = ((Number) r.get("duration_seconds")).intValue() / 60;
            String reason = null;
            if (!(Boolean) r.get("counts_for_goals")) reason = "This type of activity doesn't count";
            else if (heading != null && !heading.equals(r.get("heading_code"))) reason = "Only " + headingName + " counts";
            else if (((Number) r.get("duration_seconds")).intValue() < min * 60) reason = "Shorter than " + min + " min";
            BigDecimal kcal = (BigDecimal) r.get("kcal");
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("activityId", r.get("activity_id"));
            e.put("description", r.get("description"));
            e.put("startedAt", LocalDateTime.ofInstant(((Timestamp) r.get("started_at")).toInstant(), EvolynTime.ZONE));
            e.put("activityDate", toDate(r.get("activity_date")));
            e.put("durationMinutes", minutes);
            e.put("kcal", kcal == null ? null : kcal.setScale(0, RoundingMode.HALF_UP));
            e.put("counted", reason == null);
            e.put("reason", reason);
            out.add(e);
        }
        return out;
    }

    // ── Freezing results ─────────────────────────────────────────────────────

    private void finalizeResults(UUID challengeId, Type type, BigDecimal target, LocalDate start, LocalDate end, Map<String, Object> ch) {
        // Claim the job first, so two people opening it at once can't both write results
        int claimed = sql.update("UPDATE challenges SET finalized_at = now() WHERE challenge_id = :id AND finalized_at IS NULL",
                new MapSqlParameterSource("id", challengeId));
        if (claimed == 0) return;
        ChallengeMath.Standings s = ChallengeMath.score(type, target, tallies(challengeId, start, end, ch));
        java.util.Set<String> winningTeams = new java.util.HashSet<>();
        s.teams().stream().filter(ChallengeMath.TeamResult::won).forEach(t -> winningTeams.add(t.team()));
        for (ChallengeMath.PersonResult p : s.people()) {
            // completed = won (competitive, or on the winning team), reached the target, or the tribe did (collective)
            boolean won = switch (type) {
                case COMPETITIVE -> p.rank() == 1 && p.value().signum() > 0;
                case TEAM -> p.team() != null && winningTeams.contains(p.team());
                case COLLECTIVE -> s.completed();
                default -> p.completed();
            };
            insertResult(challengeId, "USER", p.userId(), p.team(), p.value(), p.rank(), won);
        }
        for (ChallengeMath.TeamResult t : s.teams()) {
            insertResult(challengeId, "TEAM", null, t.team(), t.value(), t.rank(), t.won());
        }
    }

    private void insertResult(UUID challengeId, String subject, UUID userId, String team, BigDecimal value, int rank, boolean completed) {
        sql.update("""
                INSERT INTO challenge_results (result_id, challenge_id, subject_type, user_id, team, value, result_rank, completed, finalized_at)
                VALUES (:rid, :id, :subject, :userId, :team, :value, :rank, :completed, now())
                """, new MapSqlParameterSource("rid", UUID.randomUUID()).addValue("id", challengeId).addValue("subject", subject)
                .addValue("userId", userId).addValue("team", team).addValue("value", value).addValue("rank", rank)
                .addValue("completed", completed));
    }

    /** The frozen results, in the same shape as live standings. */
    private ChallengeMath.Standings frozenStandings(UUID challengeId, Type type, BigDecimal target) {
        List<ChallengeMath.PersonResult> people = new ArrayList<>();
        List<ChallengeMath.TeamResult> teams = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> r : sql.queryForList("""
                SELECT subject_type, user_id, team, value, result_rank, completed
                FROM challenge_results WHERE challenge_id = :id ORDER BY result_rank, team
                """, new MapSqlParameterSource("id", challengeId))) {
            BigDecimal v = (BigDecimal) r.get("value");
            int rank = ((Number) r.get("result_rank")).intValue();
            boolean done = (Boolean) r.get("completed");
            if ("TEAM".equals(r.get("subject_type"))) {
                int members = 0;
                teams.add(new ChallengeMath.TeamResult((String) r.get("team"), v, members, rank, done));
            } else {
                BigDecimal pct = (type == Type.INDIVIDUAL_TARGET || type == Type.CONSISTENCY) && target != null
                        ? v.multiply(BigDecimal.valueOf(100)).divide(target, 1, RoundingMode.HALF_UP) : null;
                // completed = reached the target, or won (competitive / winning team); for collective
                // the shared result is on the challenge, not on each person
                people.add(new ChallengeMath.PersonResult((UUID) r.get("user_id"), (String) r.get("team"), v, pct,
                        type != Type.COLLECTIVE && done, rank));
                if (type != Type.CONSISTENCY) total = total.add(v);
            }
        }
        // Team member counts from the people rows
        List<ChallengeMath.TeamResult> withCounts = new ArrayList<>();
        for (ChallengeMath.TeamResult t : teams) {
            long n = people.stream().filter(p -> t.team().equals(p.team())).count();
            withCounts.add(new ChallengeMath.TeamResult(t.team(), t.value(), (int) n, t.rank(), t.won()));
        }
        BigDecimal totalPct = type == Type.COLLECTIVE && target != null
                ? total.multiply(BigDecimal.valueOf(100)).divide(target, 1, RoundingMode.HALF_UP) : null;
        boolean completed = type == Type.COLLECTIVE && target != null && total.compareTo(target) >= 0;
        return new ChallengeMath.Standings(people, withCounts, total, totalPct, completed);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** The challenge row, if you're a member of its tribe (and it's not someone else's draft). */
    private Map<String, Object> load(UUID challengeId, UUID userId) {
        if (userId == null) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "userId is required");
        List<Map<String, Object>> rows = sql.queryForList("SELECT * FROM challenges WHERE challenge_id = :id",
                new MapSqlParameterSource("id", challengeId));
        if (rows.isEmpty()) throw ApiProblem.of(HttpStatus.NOT_FOUND, "Challenge not found");
        Map<String, Object> ch = rows.get(0);
        tribes.requireMember((UUID) ch.get("tribe_id"), userId, false);
        if ("DRAFT".equals(ch.get("status")) && !userId.equals(ch.get("created_by"))) {
            throw ApiProblem.of(HttpStatus.NOT_FOUND, "Challenge not found");
        }
        return ch;
    }

    private static Phase phaseOf(Map<String, Object> ch) {
        return ChallengeMath.phase((String) ch.get("status"), toDate(ch.get("start_date")), toDate(ch.get("end_date")),
                EvolynTime.today(), ch.get("finalized_at") != null);
    }

    private static MapSqlParameterSource rules(UUID challengeId, LocalDate start, LocalDate end, Map<String, Object> ch) {
        return new MapSqlParameterSource("id", challengeId)
                .addValue("start", Date.valueOf(start)).addValue("end", Date.valueOf(end))
                .addValue("minSeconds", ((Number) ch.get("min_minutes")).intValue() * 60)
                .addValue("heading", ch.get("heading_code"));
    }

    private record Checked(MapSqlParameterSource params) {}

    private Checked check(ChallengeRequest r, boolean datesMustBeFuture) {
        if (r == null) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Fill in the challenge");
        String name = cleanName(r.name());
        Type type;
        try {
            type = Type.valueOf(r.type() == null ? "" : r.type());
        } catch (IllegalArgumentException e) {
            throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Choose a challenge type");
        }
        LocalDate start = r.startDate(), end = r.endDate();
        if (start == null || end == null) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Choose the start and end dates");
        if (datesMustBeFuture && start.isBefore(EvolynTime.today())) {
            throw ApiProblem.of(HttpStatus.BAD_REQUEST, "The start date can't be in the past");
        }
        if (end.isBefore(start)) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "The end date must be on or after the start");
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_DAYS) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "A challenge can last at most " + MAX_DAYS + " days");
        int min = r.minMinutes() == null ? 20 : r.minMinutes();
        if (min < 1 || min > 240) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Minimum length must be 1 to 240 minutes");

        String heading = r.headingCode() == null || r.headingCode().isBlank() ? null : r.headingCode().trim();
        if (heading != null) {
            List<Boolean> counts = sql.queryForList("SELECT counts_for_goals FROM compendium_headings WHERE heading_code = :h",
                    new MapSqlParameterSource("h", heading), Boolean.class);
            if (counts.isEmpty() || !counts.get(0)) {
                throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Choose an activity type that counts for goals");
            }
        }

        BigDecimal target = null;
        switch (type) {
            case COLLECTIVE, INDIVIDUAL_TARGET -> {
                if (r.target() == null || r.target().signum() <= 0 || r.target().compareTo(MAX_KCAL) > 0) {
                    throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Enter a target between 1 and 10,000,000 kcal");
                }
                target = r.target().setScale(0, RoundingMode.HALF_UP);
            }
            case CONSISTENCY -> {
                if (r.target() == null || r.target().signum() <= 0 || r.target().compareTo(BigDecimal.valueOf(days)) > 0) {
                    throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Active days must be between 1 and " + days);
                }
                target = r.target().setScale(0, RoundingMode.DOWN);
            }
            default -> { /* COMPETITIVE and TEAM have no target */ }
        }
        return new Checked(new MapSqlParameterSource("name", name)
                .addValue("description", cleanDescription(r.description()))
                .addValue("type", type.name())
                .addValue("metric", type == Type.CONSISTENCY ? "ACTIVE_DAYS" : "CALORIES_BURNED")
                .addValue("target", target)
                .addValue("start", Date.valueOf(start))
                .addValue("end", Date.valueOf(end))
                .addValue("min", min)
                .addValue("heading", heading));
    }

    private static String cleanName(String name) {
        String n = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (n.length() < 3 || n.length() > 80) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Name must be 3 to 80 characters");
        return n;
    }

    private static String cleanDescription(String d) {
        if (d == null || d.isBlank()) return null;
        if (d.trim().length() > 500) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Description can be at most 500 characters");
        return d.trim();
    }

    private static LocalDate toDate(Object o) {
        return o instanceof Date d ? d.toLocalDate() : (LocalDate) o;
    }
}
