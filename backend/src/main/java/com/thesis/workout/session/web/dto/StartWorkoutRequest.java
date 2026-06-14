package com.thesis.workout.session.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartWorkoutRequest(
        @NotNull UUID templateDayId,
        @NotNull UUID gymId) {
}
