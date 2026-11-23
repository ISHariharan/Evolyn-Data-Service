package com.evolyn.evolyn_data_service.Profile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evolyn.evolyn_data_service.VerifyUserDetails.VerifyUserRepository;

/**
 * Internal endpoints for Profile IDs (only Evolyn-API calls these):
 *   GET /evolyn/api/internal/profile-ids/{profileId}/availability   is it free? (for the live check)
 *   PUT /evolyn/api/internal/users/{userId}/profile-id               set your own { "profileId": "arun_k" }
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal", produces = "application/json")
public class ProfileIdController {

    private final VerifyUserRepository users;

    public ProfileIdController(VerifyUserRepository users) {
        this.users = users;
    }

    @GetMapping("/profile-ids/{profileId}/availability")
    public Map<String, Object> availability(@PathVariable String profileId) {
        String id = ProfileIds.normalize(profileId);
        String problem = ProfileIds.problem(id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("profileId", id);
        if (problem != null) {
            out.put("available", false);
            out.put("reason", problem);
        } else if (users.existsByProfileId(id)) {
            out.put("available", false);
            out.put("reason", "@" + id + " is already taken");
        } else {
            out.put("available", true);
        }
        return out;
    }

    public record ProfileIdRequest(String profileId) {}

    @PutMapping("/users/{userId}/profile-id")
    public ResponseEntity<Object> setProfileId(@PathVariable UUID userId, @RequestBody ProfileIdRequest request) {
        String id = ProfileIds.normalize(request == null ? null : request.profileId());
        String problem = ProfileIds.problem(id);
        if (problem != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", problem));
        }
        var user = users.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }
        if (id.equals(user.get().getProfileId())) {
            return ResponseEntity.ok(Map.of("profileId", id)); // already theirs
        }
        if (users.existsByProfileId(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "@" + id + " is already taken", "code", "PROFILE_ID_TAKEN"));
        }
        try {
            user.get().setProfileId(id);
            users.saveAndFlush(user.get());
        } catch (DataIntegrityViolationException raced) {
            // Someone else took it between the check and the save: the unique index caught it
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "@" + id + " is already taken", "code", "PROFILE_ID_TAKEN"));
        }
        return ResponseEntity.ok(Map.of("profileId", id));
    }
}
