package com.evolyn.evolyn_data_service.Security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Only Evolyn-API may call this service.
 *
 * On Render's free plan this service gets its own public URL, so anyone who found it
 * could call /evolyn/api/internal/... directly and read any user's data. To stop that,
 * Evolyn-API sends a shared secret in the X-Internal-Key header, and this filter
 * rejects every request without the right key (except the /health check).
 *
 * If INTERNAL_API_KEY isn't set (local development), the check is skipped.
 */
@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Internal-Key";

    private final byte[] expectedKey;

    public InternalKeyFilter(@Value("${evolyn.internal-api-key:}") String key) {
        this.expectedKey = key.isBlank() ? null : key.getBytes(StandardCharsets.UTF_8);
        if (expectedKey == null) {
            System.out.println("WARNING: INTERNAL_API_KEY is not set - internal endpoints are unprotected (fine for local development only)");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return expectedKey == null || "/health".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String given = request.getHeader(HEADER);
        // MessageDigest.isEqual compares in constant time, so the key can't be guessed
        // character by character from response timings
        if (given == null || !MessageDigest.isEqual(expectedKey, given.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Unauthorized\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
