package com.thesis.workout.marketplace.application;

import com.thesis.workout.exercise.domain.model.Visibility;
import com.thesis.workout.exercise.infrastructure.repository.ExerciseRepository;
import com.thesis.workout.marketplace.domain.model.TemplateUseEvent;
import com.thesis.workout.marketplace.infrastructure.repository.TemplateUseEventRepository;
import com.thesis.workout.template.application.TemplateService;
import com.thesis.workout.template.application.exception.TemplateNotFoundException;
import com.thesis.workout.template.domain.model.Template;
import com.thesis.workout.template.domain.model.TemplateDay;
import com.thesis.workout.template.domain.model.TemplateDayExercise;
import com.thesis.workout.template.domain.model.TemplateDayRoutine;
import com.thesis.workout.template.domain.model.TemplateStats;
import com.thesis.workout.template.domain.model.TemplateVisibility;
import com.thesis.workout.template.infrastructure.repository.TemplateDayExerciseRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateDayRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateDayRoutineRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateStatsRepository;
import com.thesis.workout.template.web.dto.TemplateDetailResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copies a PUBLIC template into a new PRIVATE template owned by the copier (Option A). Days,
 * exercises (with planned fields + muscle-group snapshots) and routines are deep-copied. The copy
 * is linked to its source only via {@code copiedFromTemplateId}: an exercise reference is kept
 * only when it points at an OFFICIAL active exercise, otherwise it is nulled (snapshots remain);
 * routine references are always nulled. The copied template is therefore fully usable from
 * snapshots and independent of the source.
 */
@Service
public class TemplateCopyService {

    private final TemplateRepository templateRepository;
    private final TemplateStatsRepository templateStatsRepository;
    private final TemplateDayRepository templateDayRepository;
    private final TemplateDayExerciseRepository templateDayExerciseRepository;
    private final TemplateDayRoutineRepository templateDayRoutineRepository;
    private final TemplateUseEventRepository templateUseEventRepository;
    private final ExerciseRepository exerciseRepository;
    private final TemplateService templateService;

    public TemplateCopyService(TemplateRepository templateRepository,
            TemplateStatsRepository templateStatsRepository, TemplateDayRepository templateDayRepository,
            TemplateDayExerciseRepository templateDayExerciseRepository,
            TemplateDayRoutineRepository templateDayRoutineRepository,
            TemplateUseEventRepository templateUseEventRepository, ExerciseRepository exerciseRepository,
            TemplateService templateService) {
        this.templateRepository = templateRepository;
        this.templateStatsRepository = templateStatsRepository;
        this.templateDayRepository = templateDayRepository;
        this.templateDayExerciseRepository = templateDayExerciseRepository;
        this.templateDayRoutineRepository = templateDayRoutineRepository;
        this.templateUseEventRepository = templateUseEventRepository;
        this.exerciseRepository = exerciseRepository;
        this.templateService = templateService;
    }

    @Transactional
    public TemplateDetailResponse use(UUID userId, UUID sourceTemplateId) {
        Template source = templateRepository
                .findByIdAndVisibilityAndDeletedAtIsNull(sourceTemplateId, TemplateVisibility.PUBLIC)
                .orElseThrow(TemplateNotFoundException::new);

        Template copy = Template.createPrivateCopy(userId, source.getName() + " (copy)", source.getDescription(),
                source.getSplitType(), source.getDaysPerWeek(), source.getDifficulty(),
                source.getEstimatedDurationMinutes(), source.getId());
        templateRepository.saveAndFlush(copy);
        templateStatsRepository.save(TemplateStats.zeroFor(copy.getId()));

        List<TemplateDay> sourceDays = templateDayRepository.findByTemplateIdOrderByDayNumberAsc(sourceTemplateId);
        List<UUID> sourceDayIds = sourceDays.stream().map(TemplateDay::getId).toList();

        Map<UUID, List<TemplateDayExercise>> exercisesByDay = sourceDayIds.isEmpty()
                ? Map.of()
                : templateDayExerciseRepository.findByTemplateDayIdInOrderByPositionAsc(sourceDayIds).stream()
                        .collect(Collectors.groupingBy(TemplateDayExercise::getTemplateDayId));
        Map<UUID, List<TemplateDayRoutine>> routinesByDay = sourceDayIds.isEmpty()
                ? Map.of()
                : templateDayRoutineRepository.findByTemplateDayIdInOrderByRoutineTypeAscPositionAsc(sourceDayIds)
                        .stream().collect(Collectors.groupingBy(TemplateDayRoutine::getTemplateDayId));

        Set<UUID> officialExerciseIds = resolveOfficialIds(exercisesByDay);

        for (TemplateDay sourceDay : sourceDays) {
            TemplateDay newDay = templateDayRepository.saveAndFlush(TemplateDay.createFor(
                    copy.getId(), sourceDay.getDayNumber(), sourceDay.getName(), sourceDay.getFocus(),
                    sourceDay.getEstimatedDurationMinutes(), sourceDay.getNotes()));

            for (TemplateDayExercise sourceExercise : exercisesByDay.getOrDefault(sourceDay.getId(), List.of())) {
                UUID keptExerciseId = sourceExercise.getExerciseId() != null
                        && officialExerciseIds.contains(sourceExercise.getExerciseId())
                        ? sourceExercise.getExerciseId()
                        : null;
                TemplateDayExercise newExercise = TemplateDayExercise.createFor(
                        newDay.getId(), keptExerciseId, sourceExercise.getExerciseNameSnapshot(),
                        sourceExercise.getExerciseTypeSnapshot(), sourceExercise.getPosition(),
                        sourceExercise.getPlannedSets(), sourceExercise.getPlannedReps(),
                        sourceExercise.getPlannedWeight(), sourceExercise.getRestSeconds(),
                        sourceExercise.getNote());
                sourceExercise.getMuscleGroups()
                        .forEach(group -> newExercise.addMuscleGroup(group.getMuscleGroupCode(), group.getRole()));
                templateDayExerciseRepository.save(newExercise);
            }

            for (TemplateDayRoutine sourceRoutine : routinesByDay.getOrDefault(sourceDay.getId(), List.of())) {
                templateDayRoutineRepository.save(TemplateDayRoutine.createFor(
                        newDay.getId(), null, sourceRoutine.getRoutineType(),
                        sourceRoutine.getRoutineNameSnapshot(), sourceRoutine.getRoutineContentSnapshot(),
                        sourceRoutine.getPosition()));
            }
        }

        templateDayExerciseRepository.flush();
        templateDayRoutineRepository.flush();
        templateRepository.recomputeAggregates(copy.getId());
        templateStatsRepository.incrementUses(sourceTemplateId);
        templateUseEventRepository.save(TemplateUseEvent.record(
                userId, sourceTemplateId, copy.getId(), source.getName(), copy.getName()));

        return templateService.get(userId, copy.getId());
    }

    private Set<UUID> resolveOfficialIds(Map<UUID, List<TemplateDayExercise>> exercisesByDay) {
        Set<UUID> referenced = new HashSet<>();
        exercisesByDay.values().forEach(list -> list.forEach(exercise -> {
            if (exercise.getExerciseId() != null) {
                referenced.add(exercise.getExerciseId());
            }
        }));
        if (referenced.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(exerciseRepository.findOfficialIdsIn(referenced, Visibility.OFFICIAL));
    }
}
