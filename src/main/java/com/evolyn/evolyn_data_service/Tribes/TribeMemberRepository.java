package com.evolyn.evolyn_data_service.Tribes;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TribeMemberRepository extends JpaRepository<TribeMemberEntity, UUID> {
    Optional<TribeMemberEntity> findByTribeIdAndUserId(UUID tribeId, UUID userId);
    long countByTribeIdAndStatus(UUID tribeId, String status);
    long countByUserIdAndStatus(UUID userId, String status);
}
