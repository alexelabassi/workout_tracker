package com.thesis.workout.template.web.dto;

import com.thesis.workout.template.domain.model.Difficulty;
import com.thesis.workout.template.domain.model.SplitType;
import com.thesis.workout.template.domain.model.Template;
import com.thesis.workout.template.domain.model.TemplateVisibility;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TemplateDetailResponse(
        UUID id,
        String name,
        String description,
        SplitType splitType,
        Integer daysPerWeek,
        Difficulty difficulty,
        Integer estimatedDurationMinutes,
        TemplateVisibility visibility,
        Instant updatedAt,
        List<String> muscleGroups,
        List<TemplateDayResponse> days) {

    public static TemplateDetailResponse from(Template template, List<String> muscleGroups,
            List<TemplateDayResponse> days) {
        return new TemplateDetailResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getSplitType(),
                template.getDaysPerWeek(),
                template.getDifficulty(),
                template.getEstimatedDurationMinutes(),
                template.getVisibility(),
                template.getUpdatedAt(),
                muscleGroups,
                days);
    }
}
