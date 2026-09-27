package com.evolyn.evolyn_data_service.Activities;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityCalculationRepository extends JpaRepository<ActivityCalculationEntity, UUID> {

    Optional<ActivityCalculationEntity> findByActivityIdAndMetricTypeAndCurrentTrue(UUID activityId, String metricType);

    // One query for a whole list of activities (no query per row)
    List<ActivityCalculationEntity> findByActivityIdInAndMetricTypeAndCurrentTrue(Collection<UUID> activityIds, String metricType);
}
