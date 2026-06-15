package com.thesis.workout.template.application;

import com.thesis.workout.exercise.application.exception.ExerciseNotFoundException;
import com.thesis.workout.exercise.domain.model.Exercise;
import com.thesis.workout.exercise.infrastructure.repository.ExerciseRepository;
import com.thesis.workout.search.application.event.TemplateIndexEvent;
import com.thesis.workout.template.domain.model.TemplateDay;
import com.thesis.workout.template.domain.model.TemplateDayExercise;
import com.thesis.workout.template.infrastructure.repository.TemplateDayExerciseRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateRepository;
import com.thesis.workout.template.web.dto.TemplateDayExerciseResponse;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds/updates/removes exercises on a template day. The referenced exercise must be visible to
 * the user (official or own custom) else 404; its name, type, and muscle groups are snapshotted.
 * After every change the parent template's aggregate arrays are recomputed. Because the recompute
 * query clears the persistence context, the response DTO is built before recompute runs.
 */
@Service
public class TemplateDayExerciseService {

    private final TemplateDayExerciseRepository templateDayExerciseRepository;
    private final TemplateRepository templateRepository;
    private final ExerciseRepository exerciseRepository;
    private final TemplateAccess templateAccess;
    private final ApplicationEventPublisher events;

    public TemplateDayExerciseService(TemplateDayExerciseRepository templateDayExerciseRepository,
            TemplateRepository templateRepository,
            ExerciseRepository exerciseRepository,
            TemplateAccess templateAccess,
            ApplicationEventPublisher events) {
        this.templateDayExerciseRepository = templateDayExerciseRepository;
        this.templateRepository = templateRepository;
        this.exerciseRepository = exerciseRepository;
        this.templateAccess = templateAccess;
        this.events = events;
    }

    @Transactional
    public TemplateDayExerciseResponse add(UUID userId, UUID dayId, TemplateDayExerciseCommand command) {
        TemplateDay day = templateAccess.requireOwnedDay(userId, dayId);
        Exercise source = visibleExercise(userId, command.exerciseId());

        TemplateDayExercise exercise = TemplateDayExercise.createFor(
                dayId, source.getId(), source.getName(), source.getExerciseType(), nextPosition(dayId),
                command.plannedSets(), normalize(command.plannedReps()), command.plannedWeight(),
                command.restSeconds(), normalize(command.note()));
        snapshotMuscleGroups(exercise, source);

        TemplateDayExercise saved = templateDayExerciseRepository.saveAndFlush(exercise);
        TemplateDayExerciseResponse response = TemplateDayExerciseResponse.from(saved);
        templateRepository.recomputeAggregates(day.getTemplateId());
        events.publishEvent(TemplateIndexEvent.structural(day.getTemplateId()));
        return response;
    }

    @Transactional
    public TemplateDayExerciseResponse update(UUID userId, UUID dayExerciseId, TemplateDayExerciseCommand command) {
        TemplateDayExercise exercise = templateAccess.requireOwnedDayExercise(userId, dayExerciseId);
        Exercise source = visibleExercise(userId, command.exerciseId());
        UUID templateId = templateAccess.templateIdOfDay(exercise.getTemplateDayId());

        exercise.updateSnapshot(source.getId(), source.getName(), source.getExerciseType());
        exercise.updatePlanned(command.plannedSets(), normalize(command.plannedReps()), command.plannedWeight(),
                command.restSeconds(), normalize(command.note()));
        // Re-snapshot muscle groups: clear and flush the old rows before inserting the new ones so
        // a re-used (exercise, code) pair never collides within the same transaction.
        exercise.clearMuscleGroups();
        templateDayExerciseRepository.flush();
        snapshotMuscleGroups(exercise, source);

        TemplateDayExercise saved = templateDayExerciseRepository.saveAndFlush(exercise);
        TemplateDayExerciseResponse response = TemplateDayExerciseResponse.from(saved);
        templateRepository.recomputeAggregates(templateId);
        events.publishEvent(TemplateIndexEvent.structural(templateId));
        return response;
    }

    @Transactional
    public void delete(UUID userId, UUID dayExerciseId) {
        TemplateDayExercise exercise = templateAccess.requireOwnedDayExercise(userId, dayExerciseId);
        UUID templateId = templateAccess.templateIdOfDay(exercise.getTemplateDayId());
        templateDayExerciseRepository.delete(exercise);
        templateRepository.recomputeAggregates(templateId);
        events.publishEvent(TemplateIndexEvent.structural(templateId));
    }

    private Exercise visibleExercise(UUID userId, UUID exerciseId) {
        return exerciseRepository.findVisibleByIdToUser(exerciseId, userId)
                .orElseThrow(ExerciseNotFoundException::new);
    }

    private void snapshotMuscleGroups(TemplateDayExercise target, Exercise source) {
        source.getMuscleGroups()
                .forEach(group -> target.addMuscleGroup(group.getMuscleGroupCode(), group.getRole()));
    }

    private int nextPosition(UUID dayId) {
        return templateDayExerciseRepository.findFirstByTemplateDayIdOrderByPositionDesc(dayId)
                .map(existing -> existing.getPosition() + 1)
                .orElse(1);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
