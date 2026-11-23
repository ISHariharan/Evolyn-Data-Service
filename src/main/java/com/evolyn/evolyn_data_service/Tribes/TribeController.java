package com.evolyn.evolyn_data_service.Tribes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal tribe endpoints (only Evolyn-API calls these). userId is always the
 * person asking, taken from their login token by Evolyn-API.
 *
 *   POST   /tribes                                   create { name, description }
 *   GET    /tribes                                   my tribes
 *   GET    /tribes/{id}                              one tribe, its members and open invitations
 *   PUT    /tribes/{id}                              edit { name, description }
 *   POST   /tribes/{id}/archive                      archive (hide, read-only)
 *   POST   /tribes/{id}/leave                        leave
 *   POST   /tribes/{id}/members/{userId}/remove      remove a member
 *   POST   /tribes/{id}/invitations                  invite { profileId }
 *   POST   /tribes/{id}/join-code                    new invite link (7 days)
 *   DELETE /tribes/{id}/join-code                    turn the link off
 *   GET    /invitations                              invitations waiting for me
 *   POST   /invitations/{id}/accept | decline | revoke
 *   GET    /tribe-codes/{code}                       what an invite link leads to
 *   POST   /tribe-codes/{code}/join                  join with an invite link
 *   GET    /profiles/{profileId}                     name behind a Profile ID (to check before inviting)
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal", produces = "application/json")
public class TribeController {

    private final TribeService service;

    public TribeController(TribeService service) {
        this.service = service;
    }

    public record TribeRequest(String name, String description) {}
    public record InviteRequest(String profileId) {}

    @PostMapping("/tribes")
    public ResponseEntity<Map<String, Object>> create(@RequestParam UUID userId, @RequestBody TribeRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(userId, r.name(), r.description()));
    }

    @GetMapping("/tribes")
    public List<Map<String, Object>> mine(@RequestParam UUID userId) {
        return service.myTribes(userId);
    }

    @GetMapping("/tribes/{tribeId}")
    public Map<String, Object> detail(@PathVariable UUID tribeId, @RequestParam UUID userId) {
        return service.detail(tribeId, userId);
    }

    @PutMapping("/tribes/{tribeId}")
    public Map<String, Object> update(@PathVariable UUID tribeId, @RequestParam UUID userId, @RequestBody TribeRequest r) {
        return service.update(tribeId, userId, r.name(), r.description());
    }

    @PostMapping("/tribes/{tribeId}/archive")
    public ResponseEntity<Void> archive(@PathVariable UUID tribeId, @RequestParam UUID userId) {
        service.archive(tribeId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tribes/{tribeId}/leave")
    public ResponseEntity<Void> leave(@PathVariable UUID tribeId, @RequestParam UUID userId) {
        service.leave(tribeId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tribes/{tribeId}/members/{memberUserId}/remove")
    public Map<String, Object> remove(@PathVariable UUID tribeId, @PathVariable UUID memberUserId, @RequestParam UUID userId) {
        return service.removeMember(tribeId, userId, memberUserId);
    }

    @PostMapping("/tribes/{tribeId}/invitations")
    public ResponseEntity<Map<String, Object>> invite(@PathVariable UUID tribeId, @RequestParam UUID userId,
                                                      @RequestBody InviteRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.invite(tribeId, userId, r.profileId()));
    }

    @PostMapping("/tribes/{tribeId}/join-code")
    public Map<String, Object> newCode(@PathVariable UUID tribeId, @RequestParam UUID userId) {
        return service.newJoinCode(tribeId, userId);
    }

    @DeleteMapping("/tribes/{tribeId}/join-code")
    public Map<String, Object> codeOff(@PathVariable UUID tribeId, @RequestParam UUID userId) {
        return service.turnOffJoinCode(tribeId, userId);
    }

    @GetMapping("/invitations")
    public List<Map<String, Object>> myInvitations(@RequestParam UUID userId) {
        return service.myInvitations(userId);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public Map<String, Object> accept(@PathVariable UUID invitationId, @RequestParam UUID userId) {
        return service.respond(invitationId, userId, true);
    }

    @PostMapping("/invitations/{invitationId}/decline")
    public Map<String, Object> decline(@PathVariable UUID invitationId, @RequestParam UUID userId) {
        return service.respond(invitationId, userId, false);
    }

    @PostMapping("/invitations/{invitationId}/revoke")
    public Map<String, Object> revoke(@PathVariable UUID invitationId, @RequestParam UUID userId) {
        return service.revokeInvitation(invitationId, userId);
    }

    @GetMapping("/tribe-codes/{code}")
    public Map<String, Object> preview(@PathVariable String code, @RequestParam UUID userId) {
        return service.previewCode(code, userId);
    }

    @PostMapping("/tribe-codes/{code}/join")
    public Map<String, Object> join(@PathVariable String code, @RequestParam UUID userId) {
        return service.joinByCode(code, userId);
    }

    @GetMapping("/profiles/{profileId}")
    public Map<String, Object> profile(@PathVariable String profileId) {
        return service.lookupProfile(profileId);
    }
}
