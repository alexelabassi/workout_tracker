package com.thesis.workout.session.infrastructure.repository;

import com.thesis.workout.session.domain.model.SessionStatus;
import com.thesis.workout.session.domain.model.WorkoutSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    Optional<WorkoutSession> findByIdAndUserId(UUID id, UUID userId);

    Optional<WorkoutSession> findByUserIdAndStatus(UUID userId, SessionStatus status);

    boolean existsByUserIdAndStatus(UUID userId, SessionStatus status);
}
