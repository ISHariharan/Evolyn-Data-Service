package com.evolyn.evolyn_data_service.Profile;

import java.util.Locale;
import java.util.Set;

/**
 * The rules for a Profile ID (e.g. "arun_k"):
 *   - 3 to 20 characters: lowercase letters, numbers and _
 *   - stored in lowercase ("Arun_K" becomes "arun_k"); a leading "@" is ignored
 *   - a few words are reserved so nobody can pretend to be the app
 */
public final class ProfileIds {

    private static final Set<String> RESERVED = Set.of(
            "admin", "administrator", "evolyn", "support", "help", "root", "system",
            "me", "null", "undefined", "tribe", "tribes", "moderator", "official");

    private ProfileIds() {}

    /** "  @Arun_K " -> "arun_k" (null stays null) */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("@")) s = s.substring(1);
        return s.toLowerCase(Locale.ROOT);
    }

    /** Why a (normalized) Profile ID isn't allowed, or null when it's fine. */
    public static String problem(String profileId) {
        if (profileId == null || profileId.isEmpty()) return "Choose a Profile ID";
        if (profileId.length() < 3 || profileId.length() > 20) return "Profile ID must be 3 to 20 characters";
        if (!profileId.matches("[a-z0-9_]+")) return "Use only lowercase letters, numbers and _";
        if (RESERVED.contains(profileId)) return "That Profile ID is reserved";
        return null;
    }
}
