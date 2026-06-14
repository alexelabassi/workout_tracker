package com.thesis.workout.template.web.dto;

import com.thesis.workout.template.domain.model.DayFocus;
import com.thesis.workout.template.domain.model.TemplateDay;
import java.util.List;
import java.util.UUID;

public record TemplateDayResponse(
        UUID id,
        int dayNumber,
        String name,
        DayFocus focus,
        Integer estimatedDurationMinutes,
        String notes,
        List<TemplateDayExerciseResponse> exercises,
        List<TemplateDayRoutineResponse> routines) {

    public static TemplateDayResponse from(TemplateDay day, List<TemplateDayExerciseResponse> exercises,
            List<TemplateDayRoutineResponse> routines) {
        return new TemplateDayResponse(
                day.getId(),
                day.getDayNumber(),
                day.getName(),
                day.getFocus(),
                day.getEstimatedDurationMinutes(),
                day.getNotes(),
                exercises,
                routines);
    }
}
