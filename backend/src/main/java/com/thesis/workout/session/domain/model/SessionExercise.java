package com.thesis.workout.session.domain.model;

import com.thesis.workout.exercise.domain.model.ExerciseType;
import com.thesis.workout.exercise.domain.model.MuscleRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * One exercise within a session: either copied from a template day at start, or added live as an
 * extra. Name/type/planned-* are snapshots; the original references are nullable (set null if the
 * source is deleted) and never read back for the session view.
 */
@Entity
@Table(name = "session_exercises")
public class SessionExercise {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "original_template_day_exercise_id")
    private UUID originalTemplateDayExerciseId;

    @Column(name = "original_exercise_id")
    private UUID originalExerciseId;

    @Column(name = "exercise_name_snapshot", nullable = false, length = 160)
    private String exerciseNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type_snapshot", nullable = false, length = 30)
    private ExerciseType exerciseTypeSnapshot;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "planned_sets_snapshot")
    private Integer plannedSetsSnapshot;

    @Column(name = "planned_reps_snapshot", length = 50)
    private String plannedRepsSnapshot;

    @Column(name = "planned_weight_snapshot", precision = 8, scale = 2)
    private BigDecimal plannedWeightSnapshot;

    @Column(name = "rest_seconds_snapshot")
    private Integer restSecondsSnapshot;

    @Column(name = "template_note_snapshot")
    private String templateNoteSnapshot;

    @Column(name = "is_extra_exercise", nullable = false)
    private boolean extraExercise;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "sessionExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionExerciseMuscleGroup> muscleGroups = new ArrayList<>();

    protected SessionExercise() {
    }

    public static SessionExercise fromTemplate(UUID sessionId, UUID templateDayExerciseId, UUID exerciseId,
            String exerciseNameSnapshot, ExerciseType exerciseTypeSnapshot, int position, Integer plannedSetsSnapshot,
            String plannedRepsSnapshot, BigDecimal plannedWeightSnapshot, Integer restSecondsSnapshot,
            String templateNoteSnapshot) {
        SessionExercise exercise = new SessionExercise();
        exercise.id = UUID.randomUUID();
        exercise.sessionId = sessionId;
        exercise.originalTemplateDayExerciseId = templateDayExerciseId;
        exercise.originalExerciseId = exerciseId;
        exercise.exerciseNameSnapshot = exerciseNameSnapshot;
        exercise.exerciseTypeSnapshot = exerciseTypeSnapshot;
        exercise.position = position;
        exercise.plannedSetsSnapshot = plannedSetsSnapshot;
        exercise.plannedRepsSnapshot = plannedRepsSnapshot;
        exercise.plannedWeightSnapshot = plannedWeightSnapshot;
        exercise.restSecondsSnapshot = restSecondsSnapshot;
        exercise.templateNoteSnapshot = templateNoteSnapshot;
        exercise.extraExercise = false;
        return exercise;
    }

    public static SessionExercise extra(UUID sessionId, UUID exerciseId, String exerciseNameSnapshot,
            ExerciseType exerciseTypeSnapshot, int position) {
        SessionExercise exercise = new SessionExercise();
        exercise.id = UUID.randomUUID();
        exercise.sessionId = sessionId;
        exercise.originalExerciseId = exerciseId;
        exercise.exerciseNameSnapshot = exerciseNameSnapshot;
        exercise.exerciseTypeSnapshot = exerciseTypeSnapshot;
        exercise.position = position;
        exercise.extraExercise = true;
        return exercise;
    }

    public void addMuscleGroup(String muscleGroupCode, MuscleRole role) {
        muscleGroups.add(SessionExerciseMuscleGroup.of(this, muscleGroupCode, role));
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getExerciseNameSnapshot() {
        return exerciseNameSnapshot;
    }

    public ExerciseType getExerciseTypeSnapshot() {
        return exerciseTypeSnapshot;
    }

    public int getPosition() {
        return position;
    }

    public Integer getPlannedSetsSnapshot() {
        return plannedSetsSnapshot;
    }

    public String getPlannedRepsSnapshot() {
        return plannedRepsSnapshot;
    }

    public BigDecimal getPlannedWeightSnapshot() {
        return plannedWeightSnapshot;
    }

    public Integer getRestSecondsSnapshot() {
        return restSecondsSnapshot;
    }

    public String getTemplateNoteSnapshot() {
        return templateNoteSnapshot;
    }

    public boolean isExtraExercise() {
        return extraExercise;
    }

    public List<SessionExerciseMuscleGroup> getMuscleGroups() {
        return muscleGroups;
    }
}
