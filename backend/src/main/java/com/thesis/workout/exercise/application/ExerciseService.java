package com.thesis.workout.exercise.application;

import com.thesis.workout.exercise.application.exception.ExerciseNameTakenException;
import com.thesis.workout.exercise.application.exception.ExerciseNotFoundException;
import com.thesis.workout.exercise.application.exception.InvalidMuscleGroupException;
import com.thesis.workout.exercise.domain.model.Exercise;
import com.thesis.workout.exercise.domain.model.MuscleGroup;
import com.thesis.workout.exercise.domain.model.Visibility;
import com.thesis.workout.exercise.infrastructure.repository.ExerciseRepository;
import com.thesis.workout.exercise.infrastructure.repository.MuscleGroupRepository;
import com.thesis.workout.exercise.web.dto.ExerciseResponse;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom-exercise use cases plus read access to the visible catalog. Every write loads the
 * target by {@code (id, ownerUserId)} so a request for another user's exercise is
 * indistinguishable from a missing one (404, no IDOR signal). Official exercises (owner null)
 * are read-only here; they are seeded by Flyway and never matched by the owner-scoped lookups.
 */
@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final MuscleGroupRepository muscleGroupRepository;

    public ExerciseService(ExerciseRepository exerciseRepository,
            MuscleGroupRepository muscleGroupRepository) {
        this.exerciseRepository = exerciseRepository;
        this.muscleGroupRepository = muscleGroupRepository;
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponse> listVisible(UUID userId) {
        Map<String, String> names = muscleGroupNames();
        return exerciseRepository.findVisibleTo(userId, Visibility.OFFICIAL).stream()
                .map(exercise -> ExerciseResponse.from(exercise, names))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponse> listOfficial() {
        Map<String, String> names = muscleGroupNames();
        return exerciseRepository.findByVisibilityAndDeletedAtIsNullOrderByNameAsc(Visibility.OFFICIAL).stream()
                .map(exercise -> ExerciseResponse.from(exercise, names))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponse> listCustom(UUID userId) {
        Map<String, String> names = muscleGroupNames();
        return exerciseRepository.findByOwnerUserIdAndDeletedAtIsNullOrderByNameAsc(userId).stream()
                .map(exercise -> ExerciseResponse.from(exercise, names))
                .toList();
    }

    @Transactional
    public ExerciseResponse createCustom(UUID userId, CustomExerciseCommand command) {
        List<MuscleAssignment> assignments = validateMuscleGroups(command.muscleGroups());
        String name = command.name().trim();
        ensureNameAvailable(userId, name, null);

        Exercise exercise = Exercise.createCustom(
                userId, name, normalizeDescription(command.description()), command.exerciseType());
        assignments.forEach(assignment -> exercise.addMuscleGroup(assignment.code(), assignment.role()));
        exerciseRepository.save(exercise);

        return ExerciseResponse.from(exercise, muscleGroupNames());
    }

    @Transactional
    public ExerciseResponse updateCustom(UUID userId, UUID exerciseId, CustomExerciseCommand command) {
        Exercise exercise = exerciseRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(exerciseId, userId)
                .orElseThrow(ExerciseNotFoundException::new);
        List<MuscleAssignment> assignments = validateMuscleGroups(command.muscleGroups());
        String name = command.name().trim();
        ensureNameAvailable(userId, name, exerciseId);

        exercise.updateDetails(name, normalizeDescription(command.description()), command.exerciseType());
        // Flush the cleared rows before re-inserting so a re-used (exercise_id, code) pair never
        // collides with the row being replaced within the same transaction.
        exercise.clearMuscleGroups();
        exerciseRepository.flush();
        assignments.forEach(assignment -> exercise.addMuscleGroup(assignment.code(), assignment.role()));

        return ExerciseResponse.from(exercise, muscleGroupNames());
    }

    @Transactional
    public void deleteCustom(UUID userId, UUID exerciseId) {
        Exercise exercise = exerciseRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(exerciseId, userId)
                .orElseThrow(ExerciseNotFoundException::new);
        exercise.softDelete(Instant.now());
    }

    private void ensureNameAvailable(UUID userId, String name, UUID excludeId) {
        exerciseRepository.findByOwnerUserIdAndNameIgnoreCaseAndDeletedAtIsNull(userId, name)
                .filter(existing -> !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new ExerciseNameTakenException();
                });
    }

    private List<MuscleAssignment> validateMuscleGroups(List<MuscleAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        for (MuscleAssignment assignment : assignments) {
            if (!seen.add(assignment.code())) {
                throw new InvalidMuscleGroupException(
                        "Muscle group '" + assignment.code() + "' is assigned more than once.");
            }
        }
        Set<String> known = muscleGroupRepository.findAll().stream()
                .map(MuscleGroup::getCode)
                .collect(Collectors.toSet());
        for (MuscleAssignment assignment : assignments) {
            if (!known.contains(assignment.code())) {
                throw new InvalidMuscleGroupException("Unknown muscle group: " + assignment.code());
            }
        }
        return assignments;
    }

    private Map<String, String> muscleGroupNames() {
        return muscleGroupRepository.findAll().stream()
                .collect(Collectors.toMap(MuscleGroup::getCode, MuscleGroup::getDisplayName));
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
