package com.evolyn.evolyn_data_service.Password;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
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
import com.evolyn.evolyn_data_service.Security.PasswordHasher;

/**
 * Recovery codes, forgotten passwords and changing your password.
 *
 *   - You create a recovery code while logged in (your current password is needed, so a
 *     borrowed session can't quietly make one). It's shown once; only its hash is kept.
 *   - "Forgot password" takes email + recovery code + new password. The answer for a wrong
 *     email and a wrong code is the same, so it doesn't tell anyone which emails exist.
 *   - A used code is replaced with a new one, returned once for the person to save.
 *   - 5 wrong codes lock the account's reset for an hour.
 */
@Service
public class PasswordService {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_FOR = Duration.ofHours(1);
    static final String NO_MATCH = "That email and recovery code don't match.";

    private final NamedParameterJdbcTemplate sql;
    private final PasswordHasher hasher;

    public PasswordService(NamedParameterJdbcTemplate sql, PasswordHasher hasher) {
        this.sql = sql;
        this.hasher = hasher;
    }

    // ── Your recovery code ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> codeStatus(UUID userId) {
        List<Timestamp> created = sql.queryForList("SELECT created_at FROM user_recovery_codes WHERE user_id = :u",
                new MapSqlParameterSource("u", userId), Timestamp.class);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("hasCode", !created.isEmpty());
        out.put("createdAt", created.isEmpty() ? null : created.get(0).toInstant());
        return out;
    }

    /** Makes a new code (replacing any old one) and returns it, once. */
    @Transactional
    public Map<String, Object> createCode(UUID userId, String currentPassword) {
        requireCurrentPassword(userId, currentPassword);
        String code = saveNewCode(userId);
        return Map.of("recoveryCode", code, "message", "Save this code somewhere safe. It's shown only now.");
    }

    // ── Forgot password ──────────────────────────────────────────────────────

    @Transactional(noRollbackFor = ApiProblem.class)
    public Map<String, Object> reset(String email, String code, String newPassword) {
        requireStrong(newPassword);
        if (email == null || email.isBlank() || !PasswordRules.looksLikeCode(code)) throw noMatch();

        List<Map<String, Object>> rows = sql.queryForList("""
                SELECT u.uuid AS user_id, r.code_hash, r.failed_attempts, r.locked_until
                FROM evolyn_user_details u JOIN user_recovery_codes r ON r.user_id = u.uuid
                WHERE lower(u.email) = lower(:email)
                """, new MapSqlParameterSource("email", email.trim()));
        if (rows.isEmpty()) throw noMatch();   // no such account, or no code made: same answer
        Map<String, Object> row = rows.get(0);
        UUID userId = (UUID) row.get("user_id");
        Instant now = Instant.now();

        Timestamp lockedUntil = (Timestamp) row.get("locked_until");
        if (lockedUntil != null && lockedUntil.toInstant().isAfter(now)) {
            // 423, not 429: Evolyn-API retries 429s (Render / Cloudflare "busy"), which would hide this message
            throw ApiProblem.of(HttpStatus.LOCKED, "Too many wrong codes. Try again in about an hour.", "LOCKED");
        }

        boolean right = MessageDigest.isEqual(
                PasswordRules.hash(code).getBytes(StandardCharsets.US_ASCII),
                ((String) row.get("code_hash")).getBytes(StandardCharsets.US_ASCII));
        if (!right) {
            int failures = ((Number) row.get("failed_attempts")).intValue() + 1;
            boolean lock = failures >= MAX_FAILURES;
            sql.update("UPDATE user_recovery_codes SET failed_attempts = :f, locked_until = :until WHERE user_id = :u",
                    new MapSqlParameterSource("u", userId).addValue("f", lock ? 0 : failures)
                            .addValue("until", lock ? Timestamp.from(now.plus(LOCK_FOR)) : null));
            throw noMatch();
        }

        setPassword(userId, newPassword);
        String next = saveNewCode(userId);   // a used code never works again
        return Map.of("message", "Password changed. Log in with your new password.", "recoveryCode", next);
    }

    // ── Change password (logged in) ──────────────────────────────────────────

    @Transactional
    public Map<String, Object> change(UUID userId, String currentPassword, String newPassword) {
        requireCurrentPassword(userId, currentPassword);
        if (currentPassword.equals(newPassword)) {
            throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Choose a password different from your current one");
        }
        requireStrong(newPassword);
        setPassword(userId, newPassword);
        return Map.of("message", "Password changed.");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void requireCurrentPassword(UUID userId, String currentPassword) {
        List<String> stored = sql.queryForList("SELECT password FROM evolyn_user_details WHERE uuid = :u",
                new MapSqlParameterSource("u", userId), String.class);
        if (stored.isEmpty()) throw ApiProblem.of(HttpStatus.NOT_FOUND, "Account not found");
        String s = stored.get(0);
        // Accounts from before hashing still hold the plain password (see VerifyUserDetails)
        boolean correct = currentPassword != null && s != null
                && (hasher.isHashed(s) ? hasher.matches(currentPassword, s) : currentPassword.equals(s));
        if (!correct) throw ApiProblem.of(HttpStatus.BAD_REQUEST, "Your current password isn't right", "WRONG_PASSWORD");
    }

    private String saveNewCode(UUID userId) {
        String code = PasswordRules.newCode();
        sql.update("""
                INSERT INTO user_recovery_codes (user_id, code_hash, created_at, failed_attempts, locked_until)
                VALUES (:u, :hash, :now, 0, NULL)
                ON CONFLICT (user_id) DO UPDATE SET code_hash = EXCLUDED.code_hash, created_at = EXCLUDED.created_at,
                                                    failed_attempts = 0, locked_until = NULL
                """, new MapSqlParameterSource("u", userId).addValue("hash", PasswordRules.hash(code))
                .addValue("now", Timestamp.from(Instant.now())));
        return code;
    }

    private static ApiProblem noMatch() {
        return ApiProblem.of(HttpStatus.BAD_REQUEST, NO_MATCH, "NO_MATCH");
    }

    private static void requireStrong(String password) {
        List<String> problems = PasswordRules.problems(password);
        if (!problems.isEmpty()) throw ApiProblem.of(HttpStatus.BAD_REQUEST, String.join(". ", problems));
    }

    private void setPassword(UUID userId, String newPassword) {
        sql.update("UPDATE evolyn_user_details SET password = :p WHERE uuid = :u",
                new MapSqlParameterSource("p", hasher.hash(newPassword)).addValue("u", userId));
    }
}
