package com.thesis.workout.template.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TemplateDayRoutineRequest(@NotNull UUID routineId) {
}
