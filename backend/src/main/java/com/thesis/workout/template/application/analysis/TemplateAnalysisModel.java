package com.thesis.workout.template.application.analysis;

import com.thesis.workout.exercise.domain.model.ExerciseType;
import com.thesis.workout.exercise.domain.model.MuscleRole;
import com.thesis.workout.template.domain.model.DayFocus;
import com.thesis.workout.template.domain.model.Difficulty;
import com.thesis.workout.template.domain.model.SplitType;
import java.util.List;
import java.util.UUID;

/**
 * Clean application-level read model the analyzer consumes, so the analyzer feature never depends
 * on web DTOs or persistence details. Produced by {@code TemplateAnalysisSource}.
 */
public final class TemplateAnalysisModel {

    private TemplateAnalysisModel() {
    }

    public record Root(
            UUID templateId,
            String name,
            SplitType splitType,
            Difficulty difficulty,
            Integer daysPerWeek,
            int authoredDayCount,
            List<Day> days) {
    }

    public record Day(UUID id, int dayNumber, String name, DayFocus focus, List<Exercise> exercises) {
    }

    public record Exercise(
            String name,
            ExerciseType type,
            Integer plannedSets,
            String plannedReps,
            Integer restSeconds,
            List<Muscle> muscles) {
    }

    public record Muscle(String code, MuscleRole role) {
    }
}
