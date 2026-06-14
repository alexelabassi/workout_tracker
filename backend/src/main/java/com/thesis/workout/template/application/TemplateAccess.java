package com.thesis.workout.template.application;

import com.thesis.workout.template.application.exception.TemplateDayExerciseNotFoundException;
import com.thesis.workout.template.application.exception.TemplateDayNotFoundException;
import com.thesis.workout.template.application.exception.TemplateDayRoutineNotFoundException;
import com.thesis.workout.template.application.exception.TemplateNotFoundException;
import com.thesis.workout.template.domain.model.Template;
import com.thesis.workout.template.domain.model.TemplateDay;
import com.thesis.workout.template.domain.model.TemplateDayExercise;
import com.thesis.workout.template.domain.model.TemplateDayRoutine;
import com.thesis.workout.template.infrastructure.repository.TemplateDayExerciseRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateDayRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateDayRoutineRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resolves nested template resources while enforcing ownership up the chain. A resource that
 * exists but belongs to another user (or to a soft-deleted template) is reported as not found at
 * the level being addressed, so there is no IDOR signal.
 */
@Component
public class TemplateAccess {

    private final TemplateRepository templateRepository;
    private final TemplateDayRepository templateDayRepository;
    private final TemplateDayExerciseRepository templateDayExerciseRepository;
    private final TemplateDayRoutineRepository templateDayRoutineRepository;

    public TemplateAccess(TemplateRepository templateRepository,
            TemplateDayRepository templateDayRepository,
            TemplateDayExerciseRepository templateDayExerciseRepository,
            TemplateDayRoutineRepository templateDayRoutineRepository) {
        this.templateRepository = templateRepository;
        this.templateDayRepository = templateDayRepository;
        this.templateDayExerciseRepository = templateDayExerciseRepository;
        this.templateDayRoutineRepository = templateDayRoutineRepository;
    }

    public Template requireOwnedTemplate(UUID userId, UUID templateId) {
        return templateRepository.findByIdAndUserIdAndDeletedAtIsNull(templateId, userId)
                .orElseThrow(TemplateNotFoundException::new);
    }

    public TemplateDay requireOwnedDay(UUID userId, UUID dayId) {
        TemplateDay day = templateDayRepository.findById(dayId)
                .orElseThrow(TemplateDayNotFoundException::new);
        if (!isOwnedTemplate(userId, day.getTemplateId())) {
            throw new TemplateDayNotFoundException();
        }
        return day;
    }

    public TemplateDayExercise requireOwnedDayExercise(UUID userId, UUID dayExerciseId) {
        TemplateDayExercise exercise = templateDayExerciseRepository.findById(dayExerciseId)
                .orElseThrow(TemplateDayExerciseNotFoundException::new);
        if (!isOwnedDay(userId, exercise.getTemplateDayId())) {
            throw new TemplateDayExerciseNotFoundException();
        }
        return exercise;
    }

    public TemplateDayRoutine requireOwnedDayRoutine(UUID userId, UUID dayRoutineId) {
        TemplateDayRoutine routine = templateDayRoutineRepository.findById(dayRoutineId)
                .orElseThrow(TemplateDayRoutineNotFoundException::new);
        if (!isOwnedDay(userId, routine.getTemplateDayId())) {
            throw new TemplateDayRoutineNotFoundException();
        }
        return routine;
    }

    public UUID templateIdOfDay(UUID dayId) {
        return templateDayRepository.findById(dayId)
                .map(TemplateDay::getTemplateId)
                .orElseThrow(TemplateDayNotFoundException::new);
    }

    private boolean isOwnedTemplate(UUID userId, UUID templateId) {
        return templateRepository.findByIdAndUserIdAndDeletedAtIsNull(templateId, userId).isPresent();
    }

    private boolean isOwnedDay(UUID userId, UUID dayId) {
        return templateDayRepository.findById(dayId)
                .map(day -> isOwnedTemplate(userId, day.getTemplateId()))
                .orElse(false);
    }
}
