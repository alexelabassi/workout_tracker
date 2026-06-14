package com.thesis.workout.session.infrastructure.repository;

import com.thesis.workout.session.domain.model.WorkoutSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, UUID> {

    List<WorkoutSet> findBySessionExerciseIdInOrderBySetNumberAsc(List<UUID> sessionExerciseIds);

    Optional<WorkoutSet> findFirstBySessionExerciseIdOrderBySetNumberDesc(UUID sessionExerciseId);
}
