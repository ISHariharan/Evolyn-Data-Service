package com.evolyn.evolyn_data_service.Tribes;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TribeRepository extends JpaRepository<TribeEntity, UUID> {
    Optional<TribeEntity> findByJoinCode(String joinCode);
    boolean existsByJoinCode(String joinCode);
}
