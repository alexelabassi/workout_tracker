package com.thesis.workout.session.application;

import com.thesis.workout.session.application.exception.SessionExerciseNotFoundException;
import com.thesis.workout.session.application.exception.WorkoutSessionNotFoundException;
import com.thesis.workout.session.application.exception.WorkoutSetNotFoundException;
import com.thesis.workout.session.domain.model.SessionExercise;
import com.thesis.workout.session.domain.model.WorkoutSession;
import com.thesis.workout.session.domain.model.WorkoutSet;
import com.thesis.workout.session.infrastructure.repository.SessionExerciseRepository;
import com.thesis.workout.session.infrastructure.repository.WorkoutSessionRepository;
import com.thesis.workout.session.infrastructure.repository.WorkoutSetRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resolves session resources while enforcing ownership through the session. A resource that
 * exists but belongs to another user is reported as not found at the level being addressed, so
 * there is no IDOR signal.
 */
@Component
public class WorkoutSessionAccess {

    private final WorkoutSessionRepository sessionRepository;
    private final SessionExerciseRepository sessionExerciseRepository;
    private final WorkoutSetRepository workoutSetRepository;

    public WorkoutSessionAccess(WorkoutSessionRepository sessionRepository,
            SessionExerciseRepository sessionExerciseRepository,
            WorkoutSetRepository workoutSetRepository) {
        this.sessionRepository = sessionRepository;
        this.sessionExerciseRepository = sessionExerciseRepository;
        this.workoutSetRepository = workoutSetRepository;
    }

    public WorkoutSession requireOwnedSession(UUID userId, UUID sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(WorkoutSessionNotFoundException::new);
    }

    public SessionExercise requireOwnedSessionExercise(UUID userId, UUID sessionExerciseId) {
        SessionExercise exercise = sessionExerciseRepository.findById(sessionExerciseId)
                .orElseThrow(SessionExerciseNotFoundException::new);
        if (!isOwnedSession(userId, exercise.getSessionId())) {
            throw new SessionExerciseNotFoundException();
        }
        return exercise;
    }

    public WorkoutSet requireOwnedSet(UUID userId, UUID setId) {
        WorkoutSet set = workoutSetRepository.findById(setId)
                .orElseThrow(WorkoutSetNotFoundException::new);
        SessionExercise exercise = sessionExerciseRepository.findById(set.getSessionExerciseId())
                .orElseThrow(WorkoutSetNotFoundException::new);
        if (!isOwnedSession(userId, exercise.getSessionId())) {
            throw new WorkoutSetNotFoundException();
        }
        return set;
    }

    private boolean isOwnedSession(UUID userId, UUID sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId).isPresent();
    }
}
