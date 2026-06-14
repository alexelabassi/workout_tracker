package com.thesis.workout.session.application;

import com.thesis.workout.gym.application.exception.EquipmentNotFoundException;
import com.thesis.workout.gym.domain.model.Equipment;
import com.thesis.workout.gym.infrastructure.repository.EquipmentRepository;
import com.thesis.workout.session.application.exception.SessionExerciseNotFoundException;
import com.thesis.workout.session.application.exception.SessionNotActiveException;
import com.thesis.workout.session.application.exception.EquipmentNotInSessionGymException;
import com.thesis.workout.session.application.exception.SetNumberConflictException;
import com.thesis.workout.session.application.exception.WorkoutSessionNotFoundException;
import com.thesis.workout.session.domain.model.SessionExercise;
import com.thesis.workout.session.domain.model.SetType;
import com.thesis.workout.session.domain.model.WorkoutSession;
import com.thesis.workout.session.domain.model.WorkoutSet;
import com.thesis.workout.session.infrastructure.repository.SessionExerciseRepository;
import com.thesis.workout.session.infrastructure.repository.WorkoutSessionRepository;
import com.thesis.workout.session.infrastructure.repository.WorkoutSetRepository;
import com.thesis.workout.session.web.dto.WorkoutSetResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logging, editing, and deleting sets. All operations require the owning session to be
 * IN_PROGRESS (else 409). Equipment is optional; when supplied it must be owned by the user and
 * belong to the session's gym, and its name is snapshotted onto the set.
 */
@Service
public class WorkoutSetService {

    private final WorkoutSetRepository workoutSetRepository;
    private final SessionExerciseRepository sessionExerciseRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final EquipmentRepository equipmentRepository;
    private final WorkoutSessionAccess access;

    public WorkoutSetService(WorkoutSetRepository workoutSetRepository,
            SessionExerciseRepository sessionExerciseRepository,
            WorkoutSessionRepository sessionRepository,
            EquipmentRepository equipmentRepository,
            WorkoutSessionAccess access) {
        this.workoutSetRepository = workoutSetRepository;
        this.sessionExerciseRepository = sessionExerciseRepository;
        this.sessionRepository = sessionRepository;
        this.equipmentRepository = equipmentRepository;
        this.access = access;
    }

    @Transactional
    public WorkoutSetResponse addSet(UUID userId, UUID sessionExerciseId, SetCommand command) {
        SessionExercise exercise = access.requireOwnedSessionExercise(userId, sessionExerciseId);
        WorkoutSession session = requireActiveSession(exercise.getSessionId());
        ResolvedEquipment equipment = resolveEquipment(userId, session.getGymId(), command.equipmentId());

        int setNumber = workoutSetRepository.findFirstBySessionExerciseIdOrderBySetNumberDesc(sessionExerciseId)
                .map(existing -> existing.getSetNumber() + 1)
                .orElse(1);
        WorkoutSet set = WorkoutSet.log(
                sessionExerciseId, setNumber, setTypeOrDefault(command), command.weight(), command.reps(),
                command.durationSeconds(), command.distanceMeters(), command.rpe(), normalize(command.note()),
                equipment.id(), equipment.name(), Instant.now());
        try {
            // UNIQUE (session_exercise_id, set_number) guards against a concurrent double-submit
            // racing on the same auto-assigned number; the loser gets a 409 and can retry.
            return WorkoutSetResponse.from(workoutSetRepository.saveAndFlush(set));
        } catch (DataIntegrityViolationException ex) {
            throw new SetNumberConflictException();
        }
    }

    @Transactional
    public WorkoutSetResponse updateSet(UUID userId, UUID setId, SetCommand command) {
        WorkoutSet set = access.requireOwnedSet(userId, setId);
        SessionExercise exercise = sessionExerciseRepository.findById(set.getSessionExerciseId())
                .orElseThrow(SessionExerciseNotFoundException::new);
        WorkoutSession session = requireActiveSession(exercise.getSessionId());
        ResolvedEquipment equipment = resolveEquipment(userId, session.getGymId(), command.equipmentId());

        set.update(setTypeOrDefault(command), command.weight(), command.reps(), command.durationSeconds(),
                command.distanceMeters(), command.rpe(), normalize(command.note()),
                equipment.id(), equipment.name());
        return WorkoutSetResponse.from(workoutSetRepository.saveAndFlush(set));
    }

    @Transactional
    public void deleteSet(UUID userId, UUID setId) {
        WorkoutSet set = access.requireOwnedSet(userId, setId);
        SessionExercise exercise = sessionExerciseRepository.findById(set.getSessionExerciseId())
                .orElseThrow(SessionExerciseNotFoundException::new);
        requireActiveSession(exercise.getSessionId());
        workoutSetRepository.delete(set);
    }

    private WorkoutSession requireActiveSession(UUID sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(WorkoutSessionNotFoundException::new);
        if (!session.isActive()) {
            throw new SessionNotActiveException();
        }
        return session;
    }

    /**
     * Resolves the optional equipment for a set. A null id clears both the reference and the name
     * snapshot. A provided id must be owned (else 404) and live in the session's gym (else 400).
     */
    private ResolvedEquipment resolveEquipment(UUID userId, UUID sessionGymId, UUID equipmentId) {
        if (equipmentId == null) {
            return new ResolvedEquipment(null, null);
        }
        Equipment equipment = equipmentRepository.findByIdAndUserIdAndDeletedAtIsNull(equipmentId, userId)
                .orElseThrow(EquipmentNotFoundException::new);
        if (sessionGymId == null || !equipment.getGymId().equals(sessionGymId)) {
            throw new EquipmentNotInSessionGymException();
        }
        return new ResolvedEquipment(equipment.getId(), equipment.getName());
    }

    private static SetType setTypeOrDefault(SetCommand command) {
        return command.setType() != null ? command.setType() : SetType.WORKING;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record ResolvedEquipment(UUID id, String name) {
    }
}
