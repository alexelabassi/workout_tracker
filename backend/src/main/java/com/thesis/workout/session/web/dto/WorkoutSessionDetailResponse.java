package com.thesis.workout.session.web.dto;

import com.thesis.workout.session.domain.model.SessionStatus;
import com.thesis.workout.session.domain.model.WorkoutSession;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutSessionDetailResponse(
        UUID id,
        SessionStatus status,
        UUID templateId,
        String templateName,
        UUID templateDayId,
        String templateDayName,
        UUID gymId,
        String gymName,
        Instant startedAt,
        Instant finishedAt,
        String notes,
        List<SessionRoutineResponse> routines,
        List<SessionExerciseResponse> exercises) {

    public static WorkoutSessionDetailResponse from(WorkoutSession session, List<SessionRoutineResponse> routines,
            List<SessionExerciseResponse> exercises) {
        return new WorkoutSessionDetailResponse(
                session.getId(),
                session.getStatus(),
                session.getTemplateId(),
                session.getTemplateNameSnapshot(),
                session.getTemplateDayId(),
                session.getTemplateDayNameSnapshot(),
                session.getGymId(),
                session.getGymNameSnapshot(),
                session.getStartedAt(),
                session.getFinishedAt(),
                session.getNotes(),
                routines,
                exercises);
    }
}
