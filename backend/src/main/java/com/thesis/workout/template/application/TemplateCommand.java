package com.thesis.workout.template.application;

import com.thesis.workout.template.domain.model.Difficulty;
import com.thesis.workout.template.domain.model.SplitType;

public record TemplateCommand(
        String name,
        String description,
        SplitType splitType,
        Integer daysPerWeek,
        Difficulty difficulty,
        Integer estimatedDurationMinutes) {
}
