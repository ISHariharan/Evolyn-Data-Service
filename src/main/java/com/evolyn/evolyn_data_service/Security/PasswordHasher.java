package com.evolyn.evolyn_data_service.Security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Passwords are never stored as typed. BCrypt turns "my-password" into something like
 * "$2a$10$N9qo8uLOickgx2ZMRZoMye..." — a one-way hash with a random salt built in,
 * so the same password gives a different hash every time and can't be reversed.
 * To check a login we hash the attempt the same way and compare (matches()).
 */
@Component
public class PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedHash) {
        return storedHash != null && isHashed(storedHash) && encoder.matches(rawPassword, storedHash);
    }

    /** Accounts created before hashing was added still hold the plain password. */
    public boolean isHashed(String stored) {
        return stored != null && stored.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }
}
