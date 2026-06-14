package com.thesis.workout.history.infrastructure.repository;

import com.thesis.workout.session.domain.model.WorkoutSession;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only access to the caller's workout sessions for the history list. A separate repository
 * (not the session package's writable one) keeps the read concern isolated. Per-session summary
 * numbers are aggregated in one batch query over a page of session ids to avoid N+1.
 */
public interface HistorySessionRepository extends Repository<WorkoutSession, UUID> {

    List<WorkoutSession> findByUserId(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    @Query(value = """
            SELECT ws.id AS sessionId,
              (SELECT count(*) FROM session_exercises se WHERE se.session_id = ws.id) AS exerciseCount,
              (SELECT count(*) FROM workout_sets s JOIN session_exercises se2 ON s.session_exercise_id = se2.id
                 WHERE se2.session_id = ws.id) AS setCount,
              (SELECT COALESCE(SUM(COALESCE(s.weight, 0) * COALESCE(s.reps, 0)), 0)
                 FROM workout_sets s JOIN session_exercises se3 ON s.session_exercise_id = se3.id
                 WHERE se3.session_id = ws.id) AS totalVolume
            FROM workout_sessions ws
            WHERE ws.id IN (:ids)
            """, nativeQuery = true)
    List<SessionSummaryRow> summarize(@Param("ids") Collection<UUID> ids);

    interface SessionSummaryRow {
        UUID getSessionId();

        long getExerciseCount();

        long getSetCount();

        BigDecimal getTotalVolume();
    }
}
