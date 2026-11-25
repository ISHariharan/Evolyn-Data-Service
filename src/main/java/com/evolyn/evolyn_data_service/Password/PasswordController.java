package com.evolyn.evolyn_data_service.Password;

import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal password endpoints (only Evolyn-API calls these):
 *   GET  /evolyn/api/internal/recovery-code?userId=     do you have a recovery code?
 *   POST /evolyn/api/internal/recovery-code?userId=     { currentPassword }  make a new one (shown once)
 *   POST /evolyn/api/internal/password-reset            { email, recoveryCode, password }
 *   POST /evolyn/api/internal/password/change?userId=   { currentPassword, newPassword }
 * Never log the bodies: they hold passwords and recovery codes.
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal", produces = "application/json")
public class PasswordController {

    private final PasswordService service;

    public PasswordController(PasswordService service) {
        this.service = service;
    }

    public record CodeRequest(String currentPassword) {}
    public record ResetRequest(String email, String recoveryCode, String password) {}
    public record ChangeRequest(String currentPassword, String newPassword) {}

    @GetMapping("/recovery-code")
    public Map<String, Object> status(@RequestParam UUID userId) {
        return service.codeStatus(userId);
    }

    @PostMapping("/recovery-code")
    public Map<String, Object> create(@RequestParam UUID userId, @RequestBody CodeRequest r) {
        return service.createCode(userId, r == null ? null : r.currentPassword());
    }

    @PostMapping("/password-reset")
    public Map<String, Object> reset(@RequestBody ResetRequest r) {
        return r == null ? service.reset(null, null, null) : service.reset(r.email(), r.recoveryCode(), r.password());
    }

    @PostMapping("/password/change")
    public Map<String, Object> change(@RequestParam UUID userId, @RequestBody ChangeRequest r) {
        return service.change(userId, r == null ? null : r.currentPassword(), r == null ? null : r.newPassword());
    }
}
