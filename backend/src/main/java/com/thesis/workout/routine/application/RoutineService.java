package com.thesis.workout.routine.application;

import com.thesis.workout.routine.application.exception.RoutineNameTakenException;
import com.thesis.workout.routine.application.exception.RoutineNotFoundException;
import com.thesis.workout.routine.domain.model.Routine;
import com.thesis.workout.routine.domain.model.RoutineType;
import com.thesis.workout.routine.infrastructure.repository.RoutineRepository;
import com.thesis.workout.routine.web.dto.RoutineResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Routine use cases. Every write loads the target by {@code (id, userId)} so a request for
 * another user's routine is indistinguishable from a missing one (404, no IDOR signal). Name
 * uniqueness is checked up front for a clean 409 and also defended by the database's partial
 * unique index, whose violation is translated to the same 409 in case of a race.
 */
@Service
public class RoutineService {

    private final RoutineRepository routineRepository;

    public RoutineService(RoutineRepository routineRepository) {
        this.routineRepository = routineRepository;
    }

    @Transactional(readOnly = true)
    public List<RoutineResponse> list(UUID userId) {
        return routineRepository.findByUserIdAndDeletedAtIsNullOrderByRoutineTypeAscNameAsc(userId).stream()
                .map(RoutineResponse::from)
                .toList();
    }

    @Transactional
    public RoutineResponse create(UUID userId, RoutineCommand command) {
        String name = command.name().trim();
        ensureNameAvailable(userId, command.routineType(), name, null);

        Routine routine = Routine.createFor(userId, name, command.routineType(), command.content());
        return RoutineResponse.from(save(routine));
    }

    @Transactional
    public RoutineResponse update(UUID userId, UUID routineId, RoutineCommand command) {
        Routine routine = routineRepository.findByIdAndUserIdAndDeletedAtIsNull(routineId, userId)
                .orElseThrow(RoutineNotFoundException::new);
        String name = command.name().trim();
        ensureNameAvailable(userId, command.routineType(), name, routineId);

        routine.updateDetails(name, command.routineType(), command.content());
        return RoutineResponse.from(save(routine));
    }

    @Transactional
    public void delete(UUID userId, UUID routineId) {
        Routine routine = routineRepository.findByIdAndUserIdAndDeletedAtIsNull(routineId, userId)
                .orElseThrow(RoutineNotFoundException::new);
        routine.softDelete(Instant.now());
    }

    private Routine save(Routine routine) {
        try {
            // Flush eagerly so a unique-index violation surfaces here (as 409) rather than at
            // commit, where it would escape the controller as an opaque 500. The returned
            // instance is the managed copy that carries the generated created/updated timestamps.
            return routineRepository.saveAndFlush(routine);
        } catch (DataIntegrityViolationException ex) {
            throw new RoutineNameTakenException();
        }
    }

    private void ensureNameAvailable(UUID userId, RoutineType routineType, String name, UUID excludeId) {
        routineRepository.findByUserIdAndRoutineTypeAndNameIgnoreCaseAndDeletedAtIsNull(userId, routineType, name)
                .filter(existing -> !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new RoutineNameTakenException();
                });
    }
}
