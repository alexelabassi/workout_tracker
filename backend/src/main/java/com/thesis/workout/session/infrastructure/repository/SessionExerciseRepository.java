package com.thesis.workout.session.infrastructure.repository;

import com.thesis.workout.session.domain.model.SessionExercise;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionExerciseRepository extends JpaRepository<SessionExercise, UUID> {

    List<SessionExercise> findBySessionIdOrderByPositionAsc(UUID sessionId);

    Optional<SessionExercise> findFirstBySessionIdOrderByPositionDesc(UUID sessionId);
}
