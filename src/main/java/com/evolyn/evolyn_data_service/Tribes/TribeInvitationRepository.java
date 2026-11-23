package com.evolyn.evolyn_data_service.Tribes;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TribeInvitationRepository extends JpaRepository<TribeInvitationEntity, UUID> {
    Optional<TribeInvitationEntity> findByTribeIdAndInvitedUserIdAndStatus(UUID tribeId, UUID invitedUserId, String status);
    long countByTribeIdAndStatus(UUID tribeId, String status);
}
