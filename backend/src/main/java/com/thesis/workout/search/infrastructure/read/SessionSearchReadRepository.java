package com.thesis.workout.search.infrastructure.read;

import com.thesis.workout.session.domain.model.WorkoutSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only helper that assembles the denormalised fields of a workout-session document from the
 * session's immutable snapshot rows. All values come from {@code *_snapshot} columns, so the search
 * document reflects what was actually trained and never depends on live exercises/gyms/equipment.
 */
public interface SessionSearchReadRepository extends Repository<WorkoutSession, UUID> {

    /** Ids of every FINISHED/CANCELLED session — the full set the workout index holds. */
    @Query(value = "SELECT id FROM workout_sessions WHERE status IN ('FINISHED', 'CANCELLED') ORDER BY started_at",
            nativeQuery = true)
    List<UUID> findIndexableSessionIds();

    @Query(value = """
            SELECT DISTINCT se.exercise_name_snapshot
            FROM session_exercises se
            WHERE se.session_id = :sessionId
            ORDER BY se.exercise_name_snapshot
            """, nativeQuery = true)
    List<String> findExerciseNames(@Param("sessionId") UUID sessionId);

    @Query(value = """
            SELECT DISTINCT mg.muscle_group_code_snapshot
            FROM session_exercise_muscle_groups mg
            JOIN session_exercises se ON se.id = mg.session_exercise_id
            WHERE se.session_id = :sessionId
            ORDER BY mg.muscle_group_code_snapshot
            """, nativeQuery = true)
    List<String> findMuscleGroupCodes(@Param("sessionId") UUID sessionId);

    @Query(value = """
            SELECT DISTINCT s.equipment_name_snapshot
            FROM workout_sets s
            JOIN session_exercises se ON se.id = s.session_exercise_id
            WHERE se.session_id = :sessionId AND s.equipment_name_snapshot IS NOT NULL
            ORDER BY s.equipment_name_snapshot
            """, nativeQuery = true)
    List<String> findEquipmentNames(@Param("sessionId") UUID sessionId);

    @Query(value = """
            SELECT
              (SELECT count(*) FROM session_exercises se WHERE se.session_id = :sessionId) AS exerciseCount,
              (SELECT count(*) FROM workout_sets s
                 JOIN session_exercises se2 ON s.session_exercise_id = se2.id
                 WHERE se2.session_id = :sessionId) AS setCount,
              (SELECT COALESCE(SUM(COALESCE(s.weight, 0) * COALESCE(s.reps, 0)), 0)
                 FROM workout_sets s
                 JOIN session_exercises se3 ON s.session_exercise_id = se3.id
                 WHERE se3.session_id = :sessionId) AS totalVolume
            """, nativeQuery = true)
    SessionSummaryRow summarize(@Param("sessionId") UUID sessionId);

    interface SessionSummaryRow {
        long getExerciseCount();

        long getSetCount();

        BigDecimal getTotalVolume();
    }
}
