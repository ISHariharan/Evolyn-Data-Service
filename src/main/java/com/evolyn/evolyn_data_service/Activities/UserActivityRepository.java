package com.evolyn.evolyn_data_service.Activities;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivityEntity, UUID> {

    // Always look rows up by id AND owner, so one person can never touch another's activity
    Optional<UserActivityEntity> findByActivityIdAndUserIdAndDeletedAtIsNull(UUID activityId, UUID userId);

    List<UserActivityEntity> findByUserIdAndDeletedAtIsNullAndActivityDateBetweenOrderByStartedAtDesc(
            UUID userId, LocalDate from, LocalDate to);

    // Activities of this person whose time overlaps [start, end), ignoring one activity (the one being edited)
    @Query(value = """
            SELECT * FROM user_activities a
            WHERE a.user_id = :userId AND a.deleted_at IS NULL
              AND a.activity_id <> :ignoreId
              AND a.started_at < :endAt
              AND a.started_at + make_interval(secs => a.duration_seconds) > :startAt
            ORDER BY a.started_at
            """, nativeQuery = true)
    List<UserActivityEntity> findOverlapping(@Param("userId") UUID userId, @Param("startAt") Instant startAt,
                                             @Param("endAt") Instant endAt, @Param("ignoreId") UUID ignoreId);
}
