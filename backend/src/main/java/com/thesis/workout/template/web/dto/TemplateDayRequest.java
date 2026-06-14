package com.thesis.workout.template.web.dto;

import com.thesis.workout.template.application.TemplateDayCommand;
import com.thesis.workout.template.domain.model.DayFocus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TemplateDayRequest(
        @NotNull @Positive Integer dayNumber,
        @NotBlank @Size(max = 160) String name,
        DayFocus focus,
        @Positive Integer estimatedDurationMinutes,
        @Size(max = 5000) String notes) {

    public TemplateDayCommand toCommand() {
        return new TemplateDayCommand(dayNumber, name, focus, estimatedDurationMinutes, notes);
    }
}
