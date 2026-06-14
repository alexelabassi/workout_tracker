package com.thesis.workout.template.application;

import com.thesis.workout.template.domain.model.DayFocus;

public record TemplateDayCommand(
        int dayNumber,
        String name,
        DayFocus focus,
        Integer estimatedDurationMinutes,
        String notes) {
}
