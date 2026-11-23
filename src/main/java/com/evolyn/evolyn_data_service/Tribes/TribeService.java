package com.evolyn.evolyn_data_service.Tribes;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evolyn.evolyn_data_service.Common.ApiProblem;
import com.evolyn.evolyn_data_service.Profile.ProfileIds;

/**
 * Everything about tribes, members, invitations and invite links.
 *
 * Rules:
 *  - Only members can see or change a tribe. For now every member can do
 *    everything (edit, invite, remove, archive): there are no roles yet.
 *  - Joining needs either an invitation the person accepts, or a valid
 *    invite link code.
 *  - Leaving or being removed keeps the membership row (LEFT/REMOVED), so the
 *    history stays; joining again reactivates it.
 *  - Archived tribes disappear from lists and can't be changed.
 *
 * Saves use saveAndFlush: the lists below are read with plain SQL, which only
 * sees changes already written to the database.
 *
 * "userId" is always the person asking. Evolyn-API takes it from their login
 * token, so it can be trusted here.
 */
@Service
public class TribeService {

    static final int MAX_MEMBERS = 50;
    static final int MAX_TRIBES_PER_PERSON = 20;
    static final int MAX_PENDING_INVITES = 30;
    static final Duration INVITE_LIFETIME = Duration.ofDays(7);
    static final Duration JOIN_CODE_LIFETIME = Duration.ofDays(7);

    // No 0/O, 1/I/L: codes are read out loud and typed on phones
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TribeRepository tribes;
    private final TribeMemberRepository members;
    private final TribeInvitationRepository invitations;
    private final NamedParameterJdbcTemplate sql;

    public TribeService(TribeRepository tribes, TribeMemberRepository members,
                        TribeInvitationRepository invitations, NamedParameterJdbcTemplate sql) {
        this.tribes = tribes;
        this.members = members;
        this.invitations = invitations;
        this.sql = sql;
    }

    // ── Tribes ───────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> create(UUID userId, String name, String description) {
        require(userId);
        if (members.countByUserIdAndStatus(userId, TribeMemberEntity.ACTIVE) >= MAX_TRIBES_PER_PERSON) {
            throw ApiProblem.of(HttpStatus.CONFLICT, "You can be in at most " + MAX_TRIBES_PER_PERSON + " tribes");
        }
        TribeEntity tribe = new TribeEntity();
        tribe.setTribeId(UUID.randomUUID());
        tribe.setName(cleanName(name));
        tribe.setDescription(cleanDescription(description));
        tribe.setCreatedBy(userId);
        tribe.setCreatedAt(Instant.now());
        tribe.setUpdatedAt(Instant.now());
        tribes.saveAndFlush(tribe);
        addMember(tribe.getTribeId(), userId);
        return detail(tribe.getTribeId(), userId);
    }

    /** The tribes you're an active member of, newest first, with member counts. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> myTribes(UUID userId) {
        require(userId);
        return sql.queryForList("""
                SELECT t.tribe_id AS "tribeId", t.name, t.description, t.created_at AS "createdAt",
                       m.joined_at AS "joinedAt",
                       (SELECT count(*) FROM tribe_members x
                         WHERE x.tribe_id = t.tribe_id AND x.status = 'ACTIVE') AS "memberCount"
                FROM tribes t
                JOIN tribe_members m ON m.tribe_id = t.tribe_id
                WHERE m.user_id = :userId AND m.status = 'ACTIVE' AND t.archived_at IS NULL
                ORDER BY m.joined_at DESC
                """, new MapSqlParameterSource("userId", userId));
    }

    /** One tribe with its members and open invitations. Members only. */
    @Transactional(readOnly = true)
    public Map<String, Object> detail(UUID tribeId, UUID userId) {
        TribeEntity tribe = requireMember(tribeId, userId, false);
        MapSqlParameterSource p = new MapSqlParameterSource("tribeId", tribeId).addValue("me", userId);

        List<Map<String, Object>> memberList = sql.queryForList("""
                SELECT u.uuid AS "userId", u.first_name AS "firstName", u.last_name AS "lastName",
                       u.profile_id AS "profileId", m.joined_at AS "joinedAt",
                       (u.uuid = :me) AS "isMe", (u.uuid = t.created_by) AS "isCreator"
                FROM tribe_members m
                JOIN tribes t ON t.tribe_id = m.tribe_id
                JOIN evolyn_user_details u ON u.uuid = m.user_id
                WHERE m.tribe_id = :tribeId AND m.status = 'ACTIVE'
                ORDER BY m.joined_at
                """, p);

        List<Map<String, Object>> pending = sql.queryForList("""
                SELECT i.invitation_id AS "invitationId", i.created_at AS "createdAt", i.expires_at AS "expiresAt",
                       u.first_name AS "firstName", u.last_name AS "lastName", u.profile_id AS "profileId",
                       b.first_name AS "invitedByFirstName", b.profile_id AS "invitedByProfileId"
                FROM tribe_invitations i
                JOIN evolyn_user_details u ON u.uuid = i.invited_user_id
                JOIN evolyn_user_details b ON b.uuid = i.invited_by
                WHERE i.tribe_id = :tribeId AND i.status = 'PENDING' AND i.expires_at > now()
                ORDER BY i.created_at DESC
                """, p);

        boolean codeActive = tribe.getJoinCode() != null && tribe.getJoinCodeExpiresAt() != null
                && tribe.getJoinCodeExpiresAt().isAfter(Instant.now());

        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("tribeId", tribe.getTribeId());
        out.put("name", tribe.getName());
        out.put("description", tribe.getDescription());
        out.put("createdAt", tribe.getCreatedAt());
        out.put("archived", tribe.getArchivedAt() != null);
        out.put("memberCount", memberList.size());
        out.put("members", memberList);
        out.put("pendingInvitations", pending);
        out.put("joinCode", codeActive ? tribe.getJoinCode() : null);
        out.put("joinCodeExpiresAt", codeActive ? tribe.getJoinCodeExpiresAt() : null);
        return out;
    }

    @Transactional
    public Map<String, Object> update(UUID tribeId, UUID userId, String name, String description) {
        TribeEntity tribe = requireMember(tribeId, userId, true);
        tribe.setName(cleanName(name));
        tribe.setDescription(cleanDescription(description));
        tribe.setUpdatedAt(Instant.now());
        tribes.saveAndFlush(tribe);
        return detail(tribeId, userId);
    }

    /** Hides the tribe for everyone and makes it read-only. Its history is kept. */
    @Transactional
    public void archive(UUID tribeId, UUID userId) {
        TribeEntity tribe = requireMember(tribeId, userId, true);
        tribe.setArchivedAt(Instant.now());
        tribe.setJoinCode(null);
        tribe.setJoinCodeExpiresAt(null);
        tribe.setUpdatedAt(Instant.now());
        tribes.saveAndFlush(tribe);
    }

    // ── Members ──────────────────────────────────────────────────────────────

    @Transactional
    public void leave(UUID tribeId, UUID userId) {
        requireMember(tribeId, userId, false);
        TribeMemberEntity m = members.findByTribeIdAndUserId(tribeId, userId).orElseThrow();
        m.setStatus(TribeMemberEntity.LEFT);
        m.setLeftAt(Instant.now());
        members.saveAndFlush(m);
    }

    @Transactional
    public Map<String, Object> removeMember(UUID tribeId, UUID userId, UUID memberUserId) {
        requireMember(tribeId, userId, true);
        if (userId.equals(memberUserId)) {
            throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Use Leave to leave the tribe yourself");
        }
        TribeMemberEntity m = members.findByTribeIdAndUserId(tribeId, memberUserId)
                .filter(x -> TribeMemberEntity.ACTIVE.equals(x.getStatus()))
                .orElseThrow(() -> ApiProblem.of(HttpStatus.NOT_FOUND, "That person isn't a member"));
        m.setStatus(TribeMemberEntity.REMOVED);
        m.setLeftAt(Instant.now());
        m.setRemovedBy(userId);
        members.saveAndFlush(m);
        return detail(tribeId, userId);
    }

    // ── Invitations by Profile ID ────────────────────────────────────────────

    /** Name and Profile ID behind a handle, so you can check who you're inviting. No email. */
    @Transactional(readOnly = true)
    public Map<String, Object> lookupProfile(String profileId) {
        String id = ProfileIds.normalize(profileId);
        List<Map<String, Object>> rows = sql.queryForList("""
                SELECT first_name AS "firstName", last_name AS "lastName", profile_id AS "profileId"
                FROM evolyn_user_details WHERE profile_id = :id
                """, new MapSqlParameterSource("id", id));
        if (rows.isEmpty()) throw ApiProblem.of(HttpStatus.NOT_FOUND, "No one has the Profile ID @" + id);
        return rows.get(0);
    }

    @Transactional
    public Map<String, Object> invite(UUID tribeId, UUID userId, String profileId) {
        requireMember(tribeId, userId, true);
        String id = ProfileIds.normalize(profileId);
        List<UUID> found = sql.queryForList("SELECT uuid FROM evolyn_user_details WHERE profile_id = :id",
                new MapSqlParameterSource("id", id), UUID.class);
        if (found.isEmpty()) throw ApiProblem.of(HttpStatus.NOT_FOUND, "No one has the Profile ID @" + id);
        UUID invitee = found.get(0);

        boolean alreadyMember = members.findByTribeIdAndUserId(tribeId, invitee)
                .map(m -> TribeMemberEntity.ACTIVE.equals(m.getStatus())).orElse(false);
        if (alreadyMember) throw ApiProblem.of(HttpStatus.CONFLICT, "@" + id + " is already in this tribe", "ALREADY_MEMBER");

        var open = invitations.findByTribeIdAndInvitedUserIdAndStatus(tribeId, invitee, TribeInvitationEntity.PENDING);
        if (open.isPresent()) {
            if (open.get().getExpiresAt().isAfter(Instant.now())) {
                throw ApiProblem.of(HttpStatus.CONFLICT, "@" + id + " already has an invitation", "ALREADY_INVITED");
            }
            // An expired one: close it so a fresh one can be sent
            open.get().setStatus(TribeInvitationEntity.REVOKED);
            open.get().setRespondedAt(Instant.now());
            invitations.saveAndFlush(open.get());
        }
        if (invitations.countByTribeIdAndStatus(tribeId, TribeInvitationEntity.PENDING) >= MAX_PENDING_INVITES) {
            throw ApiProblem.of(HttpStatus.CONFLICT, "Too many open invitations. Cancel some first.");
        }

        TribeInvitationEntity inv = new TribeInvitationEntity();
        inv.setInvitationId(UUID.randomUUID());
        inv.setTribeId(tribeId);
        inv.setInvitedUserId(invitee);
        inv.setInvitedBy(userId);
        inv.setStatus(TribeInvitationEntity.PENDING);
        inv.setCreatedAt(Instant.now());
        inv.setExpiresAt(Instant.now().plus(INVITE_LIFETIME));
        try {
            invitations.saveAndFlush(inv);
        } catch (DataIntegrityViolationException raced) {
            throw ApiProblem.of(HttpStatus.CONFLICT, "@" + id + " already has an invitation", "ALREADY_INVITED");
        }
        return detail(tribeId, userId);
    }

    @Transactional
    public Map<String, Object> revokeInvitation(UUID invitationId, UUID userId) {
        TribeInvitationEntity inv = invitations.findById(invitationId)
                .orElseThrow(() -> ApiProblem.of(HttpStatus.NOT_FOUND, "Invitation not found"));
        requireMember(inv.getTribeId(), userId, true);
        if (TribeInvitationEntity.PENDING.equals(inv.getStatus())) {
            inv.setStatus(TribeInvitationEntity.REVOKED);
            inv.setRespondedAt(Instant.now());
            invitations.saveAndFlush(inv);
        }
        return detail(inv.getTribeId(), userId);
    }

    /** Invitations waiting for you (not expired), newest first. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> myInvitations(UUID userId) {
        require(userId);
        return sql.queryForList("""
                SELECT i.invitation_id AS "invitationId", i.tribe_id AS "tribeId", t.name AS "tribeName",
                       t.description AS "tribeDescription", i.created_at AS "createdAt", i.expires_at AS "expiresAt",
                       b.first_name AS "invitedByFirstName", b.last_name AS "invitedByLastName",
                       b.profile_id AS "invitedByProfileId",
                       (SELECT count(*) FROM tribe_members x
                         WHERE x.tribe_id = t.tribe_id AND x.status = 'ACTIVE') AS "memberCount"
                FROM tribe_invitations i
                JOIN tribes t ON t.tribe_id = i.tribe_id
                JOIN evolyn_user_details b ON b.uuid = i.invited_by
                WHERE i.invited_user_id = :userId AND i.status = 'PENDING'
                  AND i.expires_at > now() AND t.archived_at IS NULL
                ORDER BY i.created_at DESC
                """, new MapSqlParameterSource("userId", userId));
    }

    /** Accept (join) or decline. Only the invited person can. */
    @Transactional
    public Map<String, Object> respond(UUID invitationId, UUID userId, boolean accept) {
        require(userId);
        TribeInvitationEntity inv = invitations.findById(invitationId)
                .filter(i -> i.getInvitedUserId().equals(userId))
                .orElseThrow(() -> ApiProblem.of(HttpStatus.NOT_FOUND, "Invitation not found"));
        if (!TribeInvitationEntity.PENDING.equals(inv.getStatus())) {
            throw ApiProblem.of(HttpStatus.CONFLICT, "This invitation was already answered or cancelled");
        }
        if (!inv.getExpiresAt().isAfter(Instant.now())) {
            throw ApiProblem.of(HttpStatus.GONE, "This invitation has expired. Ask for a new one.");
        }
        TribeEntity tribe = tribes.findById(inv.getTribeId())
                .filter(t -> t.getArchivedAt() == null)
                .orElseThrow(() -> ApiProblem.of(HttpStatus.GONE, "This tribe no longer exists"));
        inv.setStatus(accept ? TribeInvitationEntity.ACCEPTED : TribeInvitationEntity.DECLINED);
        inv.setRespondedAt(Instant.now());
        invitations.saveAndFlush(inv);
        if (!accept) return Map.of("status", "DECLINED");
        addMember(tribe.getTribeId(), userId);
        return detail(tribe.getTribeId(), userId);
    }

    // ── Invite link (join code) ──────────────────────────────────────────────

    /** Makes a new code (replacing any old one) that works for 7 days. */
    @Transactional
    public Map<String, Object> newJoinCode(UUID tribeId, UUID userId) {
        TribeEntity tribe = requireMember(tribeId, userId, true);
        String code;
        do {
            StringBuilder b = new StringBuilder(8);
            for (int i = 0; i < 8; i++) b.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            code = b.toString();
        } while (tribes.existsByJoinCode(code));
        tribe.setJoinCode(code);
        tribe.setJoinCodeExpiresAt(Instant.now().plus(JOIN_CODE_LIFETIME));
        tribe.setUpdatedAt(Instant.now());
        tribes.saveAndFlush(tribe);
        return detail(tribeId, userId);
    }

    @Transactional
    public Map<String, Object> turnOffJoinCode(UUID tribeId, UUID userId) {
        TribeEntity tribe = requireMember(tribeId, userId, true);
        tribe.setJoinCode(null);
        tribe.setJoinCodeExpiresAt(null);
        tribe.setUpdatedAt(Instant.now());
        tribes.saveAndFlush(tribe);
        return detail(tribeId, userId);
    }

    /** What a code leads to, shown before joining: tribe name and size only. */
    @Transactional(readOnly = true)
    public Map<String, Object> previewCode(String code, UUID userId) {
        require(userId);
        TribeEntity tribe = validCode(code);
        boolean member = members.findByTribeIdAndUserId(tribe.getTribeId(), userId)
                .map(m -> TribeMemberEntity.ACTIVE.equals(m.getStatus())).orElse(false);
        return Map.of(
                "tribeId", tribe.getTribeId(),
                "name", tribe.getName(),
                "description", tribe.getDescription() == null ? "" : tribe.getDescription(),
                "memberCount", members.countByTribeIdAndStatus(tribe.getTribeId(), TribeMemberEntity.ACTIVE),
                "alreadyMember", member);
    }

    @Transactional
    public Map<String, Object> joinByCode(String code, UUID userId) {
        require(userId);
        TribeEntity tribe = validCode(code);
        addMember(tribe.getTribeId(), userId);
        // An open invitation to the same tribe is now answered too
        invitations.findByTribeIdAndInvitedUserIdAndStatus(tribe.getTribeId(), userId, TribeInvitationEntity.PENDING)
                .ifPresent(i -> {
                    i.setStatus(TribeInvitationEntity.ACCEPTED);
                    i.setRespondedAt(Instant.now());
                    invitations.saveAndFlush(i);
                });
        return detail(tribe.getTribeId(), userId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** The tribe, if userId is an active member (and, when changing it, not archived). */
    TribeEntity requireMember(UUID tribeId, UUID userId, boolean forChange) {
        require(userId);
        // Same answer whether the tribe doesn't exist or you're not in it: don't reveal which
        TribeEntity tribe = tribes.findById(tribeId)
                .orElseThrow(() -> ApiProblem.of(HttpStatus.NOT_FOUND, "Tribe not found"));
        boolean member = members.findByTribeIdAndUserId(tribeId, userId)
                .map(m -> TribeMemberEntity.ACTIVE.equals(m.getStatus())).orElse(false);
        if (!member) throw ApiProblem.of(HttpStatus.NOT_FOUND, "Tribe not found");
        if (forChange && tribe.getArchivedAt() != null) {
            throw ApiProblem.of(HttpStatus.CONFLICT, "This tribe is archived and can't be changed");
        }
        return tribe;
    }

    private void addMember(UUID tribeId, UUID userId) {
        var existing = members.findByTribeIdAndUserId(tribeId, userId);
        if (existing.isPresent() && TribeMemberEntity.ACTIVE.equals(existing.get().getStatus())) return;
        if (members.countByTribeIdAndStatus(tribeId, TribeMemberEntity.ACTIVE) >= MAX_MEMBERS) {
            throw ApiProblem.of(HttpStatus.CONFLICT, "This tribe is full (" + MAX_MEMBERS + " members)");
        }
        if (members.countByUserIdAndStatus(userId, TribeMemberEntity.ACTIVE) >= MAX_TRIBES_PER_PERSON) {
            throw ApiProblem.of(HttpStatus.CONFLICT, "You can be in at most " + MAX_TRIBES_PER_PERSON + " tribes");
        }
        TribeMemberEntity m = existing.orElseGet(() -> {
            TribeMemberEntity fresh = new TribeMemberEntity();
            fresh.setMemberId(UUID.randomUUID());
            fresh.setTribeId(tribeId);
            fresh.setUserId(userId);
            return fresh;
        });
        m.setStatus(TribeMemberEntity.ACTIVE);
        m.setJoinedAt(Instant.now());
        m.setLeftAt(null);
        m.setRemovedBy(null);
        members.saveAndFlush(m);
    }

    private TribeEntity validCode(String code) {
        String c = code == null ? "" : code.trim().toUpperCase().replace("-", "");
        return tribes.findByJoinCode(c)
                .filter(t -> t.getArchivedAt() == null)
                .filter(t -> t.getJoinCodeExpiresAt() != null && t.getJoinCodeExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> ApiProblem.of(HttpStatus.NOT_FOUND,
                        "This invite link isn't valid any more. Ask for a new one.", "INVALID_CODE"));
    }

    private static void require(UUID userId) {
        if (userId == null) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "userId is required");
    }

    private static String cleanName(String name) {
        String n = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (n.length() < 2 || n.length() > 60) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Name must be 2 to 60 characters");
        return n;
    }

    private static String cleanDescription(String description) {
        if (description == null || description.isBlank()) return null;
        String d = description.trim();
        if (d.length() > 300) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Description can be at most 300 characters");
        return d;
    }
}
