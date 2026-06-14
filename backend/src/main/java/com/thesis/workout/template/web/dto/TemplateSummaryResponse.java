package com.thesis.workout.template.web.dto;

import com.thesis.workout.template.domain.model.Difficulty;
import com.thesis.workout.template.domain.model.SplitType;
import com.thesis.workout.template.domain.model.Template;
import com.thesis.workout.template.domain.model.TemplateVisibility;
import java.time.Instant;
import java.util.UUID;

public record TemplateSummaryResponse(
        UUID id,
        String name,
        String description,
        SplitType splitType,
        Integer daysPerWeek,
        Difficulty difficulty,
        Integer estimatedDurationMinutes,
        TemplateVisibility visibility,
        long dayCount,
        Instant updatedAt) {

    public static TemplateSummaryResponse from(Template template, long dayCount) {
        return new TemplateSummaryResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getSplitType(),
                template.getDaysPerWeek(),
                template.getDifficulty(),
                template.getEstimatedDurationMinutes(),
                template.getVisibility(),
                dayCount,
                template.getUpdatedAt());
    }
}
