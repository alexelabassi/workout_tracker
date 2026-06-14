package com.thesis.workout.exercise.infrastructure.repository;

import com.thesis.workout.exercise.domain.model.Exercise;
import com.thesis.workout.exercise.domain.model.Visibility;
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
}
