package com.evolyn.evolyn_data_service.Password;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Password rules and recovery-code helpers, free of databases so they can be tested.
 *
 * New passwords follow the same rules as the sign-up form ("Strong"): at least 8
 * characters with an upper-case letter, a lower-case letter and a special character.
 * At most 72 bytes, because BCrypt ignores anything after that.
 *
 * Recovery codes are 16 characters from an alphabet without look-alikes (no 0/O, 1/I/L, U),
 * shown as K7QF-9XRM-2HPA-W4TD: about 78 bits of randomness, so guessing is hopeless.
 */
public final class PasswordRules {

    private PasswordRules() {}

    public static final int MIN = 8;
    public static final int MAX_BYTES = 72;
    private static final String SPECIAL = "!@#$%^&*(),.?\":{}|<>";
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTVWXYZ23456789";
    private static final int CODE_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** What's wrong with a new password; empty when it's fine. */
    public static List<String> problems(String password) {
        List<String> out = new ArrayList<>();
        if (password == null || password.length() < MIN) out.add("Password must be at least " + MIN + " characters");
        if (password != null && password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) out.add("Password is too long");
        if (password == null || password.chars().noneMatch(Character::isUpperCase)) out.add("Add an upper-case letter");
        if (password == null || password.chars().noneMatch(Character::isLowerCase)) out.add("Add a lower-case letter");
        if (password == null || password.chars().noneMatch(c -> SPECIAL.indexOf(c) >= 0)) out.add("Add a special character, like ! @ # $ %");
        return out;
    }

    /** A new recovery code, formatted in groups of 4: K7QF-9XRM-2HPA-W4TD */
    public static String newCode() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            if (i > 0 && i % 4 == 0) b.append('-');
            b.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return b.toString();
    }

    /** How a typed code is compared: upper-case, spaces and dashes removed. */
    public static String normalize(String code) {
        return code == null ? "" : code.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    /** What's stored for a code: the SHA-256 of its normalized form, in hex. */
    public static String hash(String code) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalize(code).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** True when it could be a code at all (16 characters once normalized). */
    public static boolean looksLikeCode(String code) {
        return normalize(code).length() == CODE_LENGTH;
    }
}
