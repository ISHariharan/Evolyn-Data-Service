package com.evolyn.evolyn_data_service.Activities.Catalogue;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Read-only lookups for the activity catalogue. */
@Repository
public interface ActivityDefinitionRepository extends JpaRepository<ActivityDefinitionEntity, Long> {

    List<ActivityDefinitionEntity> findByActiveTrueOrderByActivityCodeAsc();

    // The current (active) entry for a code, e.g. 15330
    Optional<ActivityDefinitionEntity> findFirstByActivityCodeAndActiveTrue(String activityCode);
}
