package com.thesis.workout.routine.infrastructure.repository;

import com.thesis.workout.routine.domain.model.Routine;
import com.thesis.workout.routine.domain.model.RoutineType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineRepository extends JpaRepository<Routine, UUID> {

    List<Routine> findByUserIdAndDeletedAtIsNullOrderByRoutineTypeAscNameAsc(UUID userId);

    Optional<Routine> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<Routine> findByUserIdAndRoutineTypeAndNameIgnoreCaseAndDeletedAtIsNull(
            UUID userId, RoutineType routineType, String name);
}
