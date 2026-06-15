package com.thesis.workout.template.application;

import com.thesis.workout.routine.application.exception.RoutineNotFoundException;
import com.thesis.workout.routine.domain.model.Routine;
import com.thesis.workout.routine.infrastructure.repository.RoutineRepository;
import com.thesis.workout.search.application.event.TemplateIndexEvent;
import com.thesis.workout.template.domain.model.TemplateDay;
import com.thesis.workout.template.domain.model.TemplateDayRoutine;
import com.thesis.workout.template.infrastructure.repository.TemplateDayRoutineRepository;
import com.thesis.workout.template.web.dto.TemplateDayRoutineResponse;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Attaches/removes START/END routines on a template day. The referenced routine must be owned by
 * the user (else 404); its type, name, and content are snapshotted. Position is appended per
 * routine type. Routine changes do not affect template aggregates, so no recompute is needed.
 */
@Service
public class TemplateDayRoutineService {

    private final TemplateDayRoutineRepository templateDayRoutineRepository;
    private final RoutineRepository routineRepository;
    private final TemplateAccess templateAccess;
    private final ApplicationEventPublisher events;

    public TemplateDayRoutineService(TemplateDayRoutineRepository templateDayRoutineRepository,
            RoutineRepository routineRepository,
            TemplateAccess templateAccess,
            ApplicationEventPublisher events) {
        this.templateDayRoutineRepository = templateDayRoutineRepository;
        this.routineRepository = routineRepository;
        this.templateAccess = templateAccess;
        this.events = events;
    }

    @Transactional
    public TemplateDayRoutineResponse attach(UUID userId, UUID dayId, UUID routineId) {
        TemplateDay day = templateAccess.requireOwnedDay(userId, dayId);
        Routine source = routineRepository.findByIdAndUserIdAndDeletedAtIsNull(routineId, userId)
                .orElseThrow(RoutineNotFoundException::new);

        TemplateDayRoutine routine = TemplateDayRoutine.createFor(
                dayId, source.getId(), source.getRoutineType(), source.getName(), source.getContent(),
                nextPosition(dayId, source));
        TemplateDayRoutineResponse response =
                TemplateDayRoutineResponse.from(templateDayRoutineRepository.saveAndFlush(routine));
        events.publishEvent(TemplateIndexEvent.structural(day.getTemplateId()));
        return response;
    }

    @Transactional
    public void remove(UUID userId, UUID dayRoutineId) {
        TemplateDayRoutine routine = templateAccess.requireOwnedDayRoutine(userId, dayRoutineId);
        UUID templateId = templateAccess.templateIdOfDay(routine.getTemplateDayId());
        templateDayRoutineRepository.delete(routine);
        events.publishEvent(TemplateIndexEvent.structural(templateId));
    }

    private int nextPosition(UUID dayId, Routine source) {
        return templateDayRoutineRepository
                .findFirstByTemplateDayIdAndRoutineTypeOrderByPositionDesc(dayId, source.getRoutineType())
                .map(existing -> existing.getPosition() + 1)
                .orElse(1);
    }
}
