package com.thesis.workout.template.application;

import com.thesis.workout.search.application.event.TemplateIndexEvent;
import com.thesis.workout.template.application.exception.TemplateDayNumberTakenException;
import com.thesis.workout.template.domain.model.TemplateDay;
import com.thesis.workout.template.infrastructure.repository.TemplateDayExerciseRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateDayRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateDayRoutineRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateRepository;
import com.thesis.workout.template.web.dto.TemplateDayExerciseResponse;
import com.thesis.workout.template.web.dto.TemplateDayResponse;
import com.thesis.workout.template.web.dto.TemplateDayRoutineResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Template day CRUD. {@code day_number} is unique per template (pre-checked for a clean 409 and
 * defended by the DB constraint). Deleting a day cascades to its exercises/routines and triggers
 * an aggregate recompute on the parent template.
 */
@Service
public class TemplateDayService {

    private final TemplateDayRepository templateDayRepository;
    private final TemplateDayExerciseRepository templateDayExerciseRepository;
    private final TemplateDayRoutineRepository templateDayRoutineRepository;
    private final TemplateRepository templateRepository;
    private final TemplateAccess templateAccess;
    private final ApplicationEventPublisher events;

    public TemplateDayService(TemplateDayRepository templateDayRepository,
            TemplateDayExerciseRepository templateDayExerciseRepository,
            TemplateDayRoutineRepository templateDayRoutineRepository,
            TemplateRepository templateRepository,
            TemplateAccess templateAccess,
            ApplicationEventPublisher events) {
        this.templateDayRepository = templateDayRepository;
        this.templateDayExerciseRepository = templateDayExerciseRepository;
        this.templateDayRoutineRepository = templateDayRoutineRepository;
        this.templateRepository = templateRepository;
        this.templateAccess = templateAccess;
        this.events = events;
    }

    @Transactional
    public TemplateDayResponse create(UUID userId, UUID templateId, TemplateDayCommand command) {
        templateAccess.requireOwnedTemplate(userId, templateId);
        ensureDayNumberAvailable(templateId, command.dayNumber(), null);

        TemplateDay day = TemplateDay.createFor(
                templateId, command.dayNumber(), command.name().trim(), command.focus(),
                command.estimatedDurationMinutes(), normalize(command.notes()));
        TemplateDayResponse response = dayResponse(save(day));
        events.publishEvent(TemplateIndexEvent.structural(templateId));
        return response;
    }

    @Transactional
    public TemplateDayResponse update(UUID userId, UUID dayId, TemplateDayCommand command) {
        TemplateDay day = templateAccess.requireOwnedDay(userId, dayId);
        ensureDayNumberAvailable(day.getTemplateId(), command.dayNumber(), dayId);

        day.updateDetails(command.dayNumber(), command.name().trim(), command.focus(),
                command.estimatedDurationMinutes(), normalize(command.notes()));
        TemplateDayResponse response = dayResponse(save(day));
        events.publishEvent(TemplateIndexEvent.structural(day.getTemplateId()));
        return response;
    }

    @Transactional
    public void delete(UUID userId, UUID dayId) {
        TemplateDay day = templateAccess.requireOwnedDay(userId, dayId);
        UUID templateId = day.getTemplateId();
        templateDayRepository.delete(day);
        templateRepository.recomputeAggregates(templateId);
        events.publishEvent(TemplateIndexEvent.structural(templateId));
    }

    private TemplateDay save(TemplateDay day) {
        try {
            return templateDayRepository.saveAndFlush(day);
        } catch (DataIntegrityViolationException ex) {
            throw new TemplateDayNumberTakenException();
        }
    }

    private TemplateDayResponse dayResponse(TemplateDay day) {
        List<TemplateDayExerciseResponse> exercises = templateDayExerciseRepository
                .findByTemplateDayIdOrderByPositionAsc(day.getId()).stream()
                .map(TemplateDayExerciseResponse::from)
                .toList();
        List<TemplateDayRoutineResponse> routines = templateDayRoutineRepository
                .findByTemplateDayIdOrderByRoutineTypeAscPositionAsc(day.getId()).stream()
                .map(TemplateDayRoutineResponse::from)
                .toList();
        return TemplateDayResponse.from(day, exercises, routines);
    }

    private void ensureDayNumberAvailable(UUID templateId, int dayNumber, UUID excludeId) {
        templateDayRepository.findByTemplateIdAndDayNumber(templateId, dayNumber)
                .filter(existing -> !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new TemplateDayNumberTakenException();
                });
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
