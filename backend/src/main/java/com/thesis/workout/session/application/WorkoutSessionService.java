package com.thesis.workout.session.application;

import com.thesis.workout.exercise.application.exception.ExerciseNotFoundException;
import com.thesis.workout.exercise.domain.model.Exercise;
import com.thesis.workout.exercise.infrastructure.repository.ExerciseRepository;
import com.thesis.workout.gym.application.exception.GymNotFoundException;
import com.thesis.workout.gym.domain.model.Gym;
import com.thesis.workout.gym.infrastructure.repository.GymRepository;
import com.thesis.workout.session.application.exception.ActiveWorkoutExistsException;
import com.thesis.workout.session.application.exception.SessionNotActiveException;
import com.thesis.workout.session.application.exception.TemplateDayEmptyException;
import com.thesis.workout.session.domain.model.SessionExercise;
import com.thesis.workout.session.domain.model.SessionRoutine;
import com.thesis.workout.session.domain.model.SessionStatus;
import com.thesis.workout.session.domain.model.WorkoutSession;
import com.thesis.workout.session.domain.model.WorkoutSet;
import com.thesis.workout.session.infrastructure.repository.SessionExerciseRepository;
import com.thesis.workout.session.infrastructure.repository.SessionRoutineRepository;
import com.thesis.workout.session.infrastructure.repository.WorkoutSessionRepository;
import com.thesis.workout.session.infrastructure.repository.WorkoutSetRepository;
import com.thesis.workout.session.web.dto.SessionExerciseResponse;
import com.thesis.workout.session.web.dto.SessionRoutineResponse;
import com.thesis.workout.session.web.dto.WorkoutSessionDetailResponse;
import com.thesis.workout.session.web.dto.WorkoutSetResponse;
import com.thesis.workout.template.application.TemplateAccess;
import com.thesis.workout.template.domain.model.Template;
import com.thesis.workout.template.domain.model.TemplateDay;
import com.thesis.workout.template.domain.model.TemplateDayExercise;
import com.thesis.workout.template.domain.model.TemplateDayRoutine;
import com.thesis.workout.template.infrastructure.repository.TemplateDayExerciseRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateDayRoutineRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Live workout lifecycle. {@code start} copies the chosen template day into immutable session
 * snapshots in a single transaction (CLAUDE.md rule 9); thereafter the session never reads the
 * live planning tables, so source edits/deletes cannot change recorded history. Only one
 * IN_PROGRESS session per user is allowed.
 */
@Service
public class WorkoutSessionService {

    private final WorkoutSessionRepository sessionRepository;
    private final SessionExerciseRepository sessionExerciseRepository;
    private final SessionRoutineRepository sessionRoutineRepository;
    private final WorkoutSetRepository workoutSetRepository;
    private final WorkoutSessionAccess access;
    private final TemplateAccess templateAccess;
    private final TemplateDayExerciseRepository templateDayExerciseRepository;
    private final TemplateDayRoutineRepository templateDayRoutineRepository;
    private final ExerciseRepository exerciseRepository;
    private final GymRepository gymRepository;

    public WorkoutSessionService(WorkoutSessionRepository sessionRepository,
            SessionExerciseRepository sessionExerciseRepository,
            SessionRoutineRepository sessionRoutineRepository,
            WorkoutSetRepository workoutSetRepository,
            WorkoutSessionAccess access,
            TemplateAccess templateAccess,
            TemplateDayExerciseRepository templateDayExerciseRepository,
            TemplateDayRoutineRepository templateDayRoutineRepository,
            ExerciseRepository exerciseRepository,
            GymRepository gymRepository) {
        this.sessionRepository = sessionRepository;
        this.sessionExerciseRepository = sessionExerciseRepository;
        this.sessionRoutineRepository = sessionRoutineRepository;
        this.workoutSetRepository = workoutSetRepository;
        this.access = access;
        this.templateAccess = templateAccess;
        this.templateDayExerciseRepository = templateDayExerciseRepository;
        this.templateDayRoutineRepository = templateDayRoutineRepository;
        this.exerciseRepository = exerciseRepository;
        this.gymRepository = gymRepository;
    }

    @Transactional
    public WorkoutSessionDetailResponse start(UUID userId, UUID templateDayId, UUID gymId) {
        if (sessionRepository.existsByUserIdAndStatus(userId, SessionStatus.IN_PROGRESS)) {
            throw new ActiveWorkoutExistsException();
        }
        TemplateDay day = templateAccess.requireOwnedDay(userId, templateDayId);
        Template template = templateAccess.requireOwnedTemplate(userId, day.getTemplateId());
        Gym gym = gymRepository.findByIdAndUserIdAndDeletedAtIsNull(gymId, userId)
                .orElseThrow(GymNotFoundException::new);

        List<TemplateDayExercise> dayExercises =
                templateDayExerciseRepository.findByTemplateDayIdOrderByPositionAsc(templateDayId);
        if (dayExercises.isEmpty()) {
            throw new TemplateDayEmptyException();
        }

        WorkoutSession session = WorkoutSession.start(
                userId, template.getId(), day.getId(), template.getName(), day.getName(),
                gym.getId(), gym.getName());
        try {
            // The partial unique index ux_workout_sessions_one_active_per_user is the real
            // guarantee: if a concurrent start won the race, this flush fails and we report 409.
            sessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException ex) {
            throw new ActiveWorkoutExistsException();
        }

        for (TemplateDayExercise dayExercise : dayExercises) {
            SessionExercise sessionExercise = SessionExercise.fromTemplate(
                    session.getId(), dayExercise.getId(), dayExercise.getExerciseId(),
                    dayExercise.getExerciseNameSnapshot(), dayExercise.getExerciseTypeSnapshot(),
                    dayExercise.getPosition(), dayExercise.getPlannedSets(), dayExercise.getPlannedReps(),
                    dayExercise.getPlannedWeight(), dayExercise.getRestSeconds(), dayExercise.getNote());
            dayExercise.getMuscleGroups()
                    .forEach(group -> sessionExercise.addMuscleGroup(group.getMuscleGroupCode(), group.getRole()));
            sessionExerciseRepository.save(sessionExercise);
        }

        templateDayRoutineRepository.findByTemplateDayIdOrderByRoutineTypeAscPositionAsc(templateDayId)
                .forEach(dayRoutine -> sessionRoutineRepository.save(SessionRoutine.fromTemplate(
                        session.getId(), dayRoutine.getRoutineId(), dayRoutine.getRoutineType(),
                        dayRoutine.getRoutineNameSnapshot(), dayRoutine.getRoutineContentSnapshot(),
                        dayRoutine.getPosition())));

        sessionExerciseRepository.flush();
        sessionRoutineRepository.flush();
        return assembleDetail(session);
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutSessionDetailResponse> active(UUID userId) {
        return sessionRepository.findByUserIdAndStatus(userId, SessionStatus.IN_PROGRESS)
                .map(this::assembleDetail);
    }

    @Transactional(readOnly = true)
    public WorkoutSessionDetailResponse get(UUID userId, UUID sessionId) {
        return assembleDetail(access.requireOwnedSession(userId, sessionId));
    }

    @Transactional
    public WorkoutSessionDetailResponse finish(UUID userId, UUID sessionId) {
        WorkoutSession session = requireActiveSession(userId, sessionId);
        session.finish(Instant.now());
        return assembleDetail(session);
    }

    @Transactional
    public WorkoutSessionDetailResponse cancel(UUID userId, UUID sessionId) {
        WorkoutSession session = requireActiveSession(userId, sessionId);
        session.cancel(Instant.now());
        return assembleDetail(session);
    }

    @Transactional
    public SessionExerciseResponse addExtraExercise(UUID userId, UUID sessionId, UUID exerciseId) {
        WorkoutSession session = requireActiveSession(userId, sessionId);
        Exercise source = exerciseRepository.findVisibleByIdToUser(exerciseId, userId)
                .orElseThrow(ExerciseNotFoundException::new);

        int position = sessionExerciseRepository.findFirstBySessionIdOrderByPositionDesc(session.getId())
                .map(existing -> existing.getPosition() + 1)
                .orElse(1);
        SessionExercise sessionExercise = SessionExercise.extra(
                session.getId(), source.getId(), source.getName(), source.getExerciseType(), position);
        source.getMuscleGroups()
                .forEach(group -> sessionExercise.addMuscleGroup(group.getMuscleGroupCode(), group.getRole()));

        SessionExercise saved = sessionExerciseRepository.saveAndFlush(sessionExercise);
        return SessionExerciseResponse.from(saved, List.of());
    }

    private WorkoutSession requireActiveSession(UUID userId, UUID sessionId) {
        WorkoutSession session = access.requireOwnedSession(userId, sessionId);
        if (!session.isActive()) {
            throw new SessionNotActiveException();
        }
        return session;
    }

    private WorkoutSessionDetailResponse assembleDetail(WorkoutSession session) {
        List<SessionRoutineResponse> routines =
                sessionRoutineRepository.findBySessionIdOrderByRoutineTypeAscPositionAsc(session.getId()).stream()
                        .map(SessionRoutineResponse::from)
                        .toList();

        List<SessionExercise> exercises = sessionExerciseRepository.findBySessionIdOrderByPositionAsc(session.getId());
        List<UUID> exerciseIds = exercises.stream().map(SessionExercise::getId).toList();
        Map<UUID, List<WorkoutSetResponse>> setsByExercise = exerciseIds.isEmpty()
                ? Map.of()
                : workoutSetRepository.findBySessionExerciseIdInOrderBySetNumberAsc(exerciseIds).stream()
                        .collect(Collectors.groupingBy(
                                WorkoutSet::getSessionExerciseId,
                                Collectors.mapping(WorkoutSetResponse::from, Collectors.toList())));

        List<SessionExerciseResponse> exerciseResponses = exercises.stream()
                .map(exercise -> SessionExerciseResponse.from(
                        exercise, setsByExercise.getOrDefault(exercise.getId(), List.of())))
                .toList();

        return WorkoutSessionDetailResponse.from(session, routines, exerciseResponses);
    }
}
