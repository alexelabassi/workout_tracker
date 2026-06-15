package com.thesis.workout.search.application;

import com.thesis.workout.search.application.document.WorkoutSessionDocument;
import com.thesis.workout.search.infrastructure.read.SessionSearchReadRepository;
import com.thesis.workout.search.infrastructure.read.SessionSearchReadRepository.SessionSummaryRow;
import com.thesis.workout.session.domain.model.SessionStatus;
import com.thesis.workout.session.domain.model.WorkoutSession;
import com.thesis.workout.session.infrastructure.repository.WorkoutSessionRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds a {@link WorkoutSessionDocument} from a session's immutable snapshot rows. Only
 * FINISHED/CANCELLED sessions are indexable — an in-progress workout is not history yet.
 */
@Service
public class WorkoutSessionDocumentAssembler {

    private final WorkoutSessionRepository workoutSessionRepository;
    private final SessionSearchReadRepository sessionSearchReadRepository;

    public WorkoutSessionDocumentAssembler(WorkoutSessionRepository workoutSessionRepository,
            SessionSearchReadRepository sessionSearchReadRepository) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.sessionSearchReadRepository = sessionSearchReadRepository;
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutSessionDocument> assemble(UUID sessionId) {
        Optional<WorkoutSession> maybe = workoutSessionRepository.findById(sessionId);
        if (maybe.isEmpty() || maybe.get().getStatus() == SessionStatus.IN_PROGRESS) {
            return Optional.empty();
        }
        WorkoutSession session = maybe.get();

        List<String> exerciseNames = sessionSearchReadRepository.findExerciseNames(sessionId);
        List<String> muscleGroups = sessionSearchReadRepository.findMuscleGroupCodes(sessionId);
        List<String> equipmentNames = sessionSearchReadRepository.findEquipmentNames(sessionId);
        SessionSummaryRow summary = sessionSearchReadRepository.summarize(sessionId);

        Long durationSeconds = session.getFinishedAt() != null
                ? Duration.between(session.getStartedAt(), session.getFinishedAt()).toSeconds()
                : null;
        BigDecimal totalVolume = summary != null && summary.getTotalVolume() != null
                ? summary.getTotalVolume()
                : BigDecimal.ZERO;

        return Optional.of(new WorkoutSessionDocument(
                session.getId().toString(),
                session.getUserId().toString(),
                session.getStatus().name(),
                epochMillis(session.getStartedAt()),
                epochMillis(session.getFinishedAt()),
                durationSeconds,
                session.getTemplateNameSnapshot(),
                session.getTemplateDayNameSnapshot(),
                session.getGymNameSnapshot(),
                exerciseNames,
                muscleGroups,
                String.join(" ", muscleGroups),
                equipmentNames,
                session.getNotes(),
                totalVolume.doubleValue(),
                summary != null ? (int) summary.getSetCount() : 0,
                summary != null ? (int) summary.getExerciseCount() : 0));
    }

    private static Long epochMillis(Instant instant) {
        return instant != null ? instant.toEpochMilli() : null;
    }
}
