package com.thesis.workout.exercise.infrastructure.repository;

import com.thesis.workout.exercise.domain.model.Exercise;
import com.thesis.workout.exercise.domain.model.Visibility;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    List<Exercise> findByVisibilityAndDeletedAtIsNullOrderByNameAsc(Visibility visibility);

    List<Exercise> findByOwnerUserIdAndDeletedAtIsNullOrderByNameAsc(UUID ownerUserId);

    Optional<Exercise> findByIdAndOwnerUserIdAndDeletedAtIsNull(UUID id, UUID ownerUserId);

    Optional<Exercise> findByOwnerUserIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID ownerUserId, String name);

    /** Everything the user may see: the official catalog plus their own custom exercises. */
    @Query("SELECT e FROM Exercise e WHERE e.deletedAt IS NULL "
            + "AND (e.visibility = :official OR e.ownerUserId = :userId) "
            + "ORDER BY e.name ASC")
    List<Exercise> findVisibleTo(@Param("userId") UUID userId, @Param("official") Visibility official);

    /**
     * A single exercise the user may reference: official (owner null) or their own custom one.
     * Used when snapshotting an exercise into a template day.
     */
    @Query("SELECT e FROM Exercise e WHERE e.id = :id AND e.deletedAt IS NULL "
            + "AND (e.ownerUserId IS NULL OR e.ownerUserId = :userId)")
    Optional<Exercise> findVisibleByIdToUser(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * Filters a set of exercise ids down to those that are OFFICIAL and active. Used when copying
     * a public template: only official references are safe to keep; foreign/custom ones are nulled.
     */
    @Query("SELECT e.id FROM Exercise e WHERE e.id IN :ids AND e.deletedAt IS NULL AND e.visibility = :official")
    List<UUID> findOfficialIdsIn(@Param("ids") Collection<UUID> ids, @Param("official") Visibility official);
}
