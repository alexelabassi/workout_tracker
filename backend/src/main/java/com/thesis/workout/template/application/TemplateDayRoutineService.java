package com.thesis.workout.template.application;

import com.thesis.workout.routine.application.exception.RoutineNotFoundException;
import com.thesis.workout.routine.domain.model.Routine;
import com.thesis.workout.routine.infrastructure.repository.RoutineRepository;
import com.thesis.workout.template.domain.model.TemplateDayRoutine;
import com.thesis.workout.template.infrastructure.repository.TemplateDayRoutineRepository;
import com.thesis.workout.template.web.dto.TemplateDayRoutineResponse;
import java.util.UUID;
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

    public TemplateDayRoutineService(TemplateDayRoutineRepository templateDayRoutineRepository,
            RoutineRepository routineRepository,
            TemplateAccess templateAccess) {
        this.templateDayRoutineRepository = templateDayRoutineRepository;
        this.routineRepository = routineRepository;
        this.templateAccess = templateAccess;
    }

    @Transactional
    public TemplateDayRoutineResponse attach(UUID userId, UUID dayId, UUID routineId) {
        templateAccess.requireOwnedDay(userId, dayId);
        Routine source = routineRepository.findByIdAndUserIdAndDeletedAtIsNull(routineId, userId)
                .orElseThrow(RoutineNotFoundException::new);

        TemplateDayRoutine routine = TemplateDayRoutine.createFor(
                dayId, source.getId(), source.getRoutineType(), source.getName(), source.getContent(),
                nextPosition(dayId, source));
        return TemplateDayRoutineResponse.from(templateDayRoutineRepository.saveAndFlush(routine));
    }

    @Transactional
    public void remove(UUID userId, UUID dayRoutineId) {
        TemplateDayRoutine routine = templateAccess.requireOwnedDayRoutine(userId, dayRoutineId);
        templateDayRoutineRepository.delete(routine);
    }

    private int nextPosition(UUID dayId, Routine source) {
        return templateDayRoutineRepository
                .findFirstByTemplateDayIdAndRoutineTypeOrderByPositionDesc(dayId, source.getRoutineType())
                .map(existing -> existing.getPosition() + 1)
                .orElse(1);
    }
}
