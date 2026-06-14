package com.thesis.workout.template.web.dto;

import com.thesis.workout.template.application.TemplateCommand;
import com.thesis.workout.template.domain.model.Difficulty;
import com.thesis.workout.template.domain.model.SplitType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TemplateRequest(
        @NotBlank @Size(max = 180) String name,
        @Size(max = 5000) String description,
        SplitType splitType,
        @Min(1) @Max(7) Integer daysPerWeek,
        Difficulty difficulty,
        @Positive Integer estimatedDurationMinutes) {

    public TemplateCommand toCommand() {
        return new TemplateCommand(name, description, splitType, daysPerWeek, difficulty, estimatedDurationMinutes);
    }
}
