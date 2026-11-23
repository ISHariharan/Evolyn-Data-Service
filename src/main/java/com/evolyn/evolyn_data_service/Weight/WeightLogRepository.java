package com.evolyn.evolyn_data_service.Weight;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeightLogRepository extends JpaRepository<WeightLogEntity, UUID> {

    Optional<WeightLogEntity> findByUserIdAndRecordedOn(UUID userId, LocalDate recordedOn);

    // The weight that applies on a given day: the latest entry on or before it
    Optional<WeightLogEntity> findFirstByUserIdAndRecordedOnLessThanEqualOrderByRecordedOnDesc(UUID userId, LocalDate day);

    // The first weight someone ever saved (for activities from before it)
    Optional<WeightLogEntity> findFirstByUserIdOrderByRecordedOnAsc(UUID userId);

    List<WeightLogEntity> findByUserIdAndRecordedOnBetweenOrderByRecordedOnDesc(UUID userId, LocalDate from, LocalDate to);
}
