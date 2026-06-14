package com.thesis.workout.analytics.infrastructure.repository;

import com.thesis.workout.session.domain.model.WorkoutSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only aggregations over the caller's FINISHED sessions. All grouping/ranking is done
 * Postgres-side; dates are returned as ISO strings to avoid JDBC date-type coercion in
 * projections. Counts come back as bigint (long); money/volume as numeric (BigDecimal).
 */
public interface AnalyticsQueryRepository extends Repository<WorkoutSession, UUID> {

    /** Total logged volume (Σ weight×reps) per calendar day, one point per day, chronological. */
    @Query(value = """
            SELECT to_char(ws.started_at::date, 'YYYY-MM-DD') AS day,
                   COALESCE(SUM(COALESCE(s.weight, 0) * COALESCE(s.reps, 0)), 0) AS volume
            FROM workout_sessions ws
            JOIN session_exercises se ON se.session_id = ws.id
            JOIN workout_sets s ON s.session_exercise_id = se.id
            WHERE ws.user_id = :userId AND ws.status = 'FINISHED'
            GROUP BY ws.started_at::date
            ORDER BY ws.started_at::date
            """, nativeQuery = true)
    List<VolumePoint> volumeOverTime(@Param("userId") UUID userId);

    /** Count of finished workouts per ISO week (Monday-anchored). */
    @Query(value = """
            SELECT to_char(date_trunc('week', ws.started_at)::date, 'YYYY-MM-DD') AS weekStart,
                   count(*) AS workouts
            FROM workout_sessions ws
            WHERE ws.user_id = :userId AND ws.status = 'FINISHED'
            GROUP BY date_trunc('week', ws.started_at)
            ORDER BY date_trunc('week', ws.started_at)
            """, nativeQuery = true)
    List<WeeklyCount> workoutsPerWeek(@Param("userId") UUID userId);

    /** Number of logged sets whose exercise lists each muscle group as a PRIMARY mover. */
    @Query(value = """
            SELECT mg.muscle_group_code_snapshot AS code, count(*) AS setCount
            FROM workout_sessions ws
            JOIN session_exercises se ON se.session_id = ws.id
            JOIN session_exercise_muscle_groups mg ON mg.session_exercise_id = se.id
            JOIN workout_sets s ON s.session_exercise_id = se.id
            WHERE ws.user_id = :userId AND ws.status = 'FINISHED' AND mg.role_snapshot = 'PRIMARY'
            GROUP BY mg.muscle_group_code_snapshot
            ORDER BY count(*) DESC
            """, nativeQuery = true)
    List<MuscleCount> primaryMuscleSetDistribution(@Param("userId") UUID userId);

    /**
     * Best set per exercise, ranked by estimated 1RM (Epley: weight × (1 + reps/30)) so a heavy
     * single doesn't beat a strong rep set. Only weighted, repped sets qualify.
     */
    @Query(value = """
            SELECT DISTINCT ON (se.exercise_name_snapshot)
                   se.exercise_name_snapshot AS exerciseName,
                   s.weight AS weight,
                   s.reps AS reps,
                   round((s.weight * (1 + s.reps / 30.0))::numeric, 1) AS estimatedOneRepMax,
                   ws.id AS sessionId,
                   to_char(s.completed_at, 'YYYY-MM-DD') AS performedAt
            FROM workout_sessions ws
            JOIN session_exercises se ON se.session_id = ws.id
            JOIN workout_sets s ON s.session_exercise_id = se.id
            WHERE ws.user_id = :userId AND ws.status = 'FINISHED'
              AND s.weight IS NOT NULL AND s.reps IS NOT NULL AND s.reps > 0
            ORDER BY se.exercise_name_snapshot, (s.weight * (1 + s.reps / 30.0)) DESC, s.weight DESC
            """, nativeQuery = true)
    List<BestSetRow> bestSetsPerExercise(@Param("userId") UUID userId);

    /**
     * Best estimated 1RM per exercise per day, for plotting strength progression lines over time.
     * One point per (day, exercise); only weighted, repped sets qualify. Ordered by exercise then
     * day so the service can group into per-exercise, date-ordered series.
     */
    @Query(value = """
            SELECT to_char(ws.started_at::date, 'YYYY-MM-DD') AS day,
                   se.exercise_name_snapshot AS exerciseName,
                   round(max(s.weight * (1 + s.reps / 30.0))::numeric, 1) AS estimatedOneRepMax
            FROM workout_sessions ws
            JOIN session_exercises se ON se.session_id = ws.id
            JOIN workout_sets s ON s.session_exercise_id = se.id
            WHERE ws.user_id = :userId AND ws.status = 'FINISHED'
              AND s.weight IS NOT NULL AND s.reps IS NOT NULL AND s.reps > 0
            GROUP BY ws.started_at::date, se.exercise_name_snapshot
            ORDER BY se.exercise_name_snapshot, ws.started_at::date
            """, nativeQuery = true)
    List<OneRepMaxPoint> oneRepMaxOverTime(@Param("userId") UUID userId);

    interface VolumePoint {
        String getDay();

        BigDecimal getVolume();
    }

    interface OneRepMaxPoint {
        String getDay();

        String getExerciseName();

        BigDecimal getEstimatedOneRepMax();
    }

    interface WeeklyCount {
        String getWeekStart();

        long getWorkouts();
    }

    interface MuscleCount {
        String getCode();

        long getSetCount();
    }

    interface BestSetRow {
        String getExerciseName();

        BigDecimal getWeight();

        Integer getReps();

        BigDecimal getEstimatedOneRepMax();

        UUID getSessionId();

        String getPerformedAt();
    }
}
