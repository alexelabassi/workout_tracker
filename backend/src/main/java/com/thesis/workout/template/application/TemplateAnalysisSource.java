package com.thesis.workout.template.application;

import com.thesis.workout.template.application.analysis.TemplateAnalysisModel;
import com.thesis.workout.template.application.exception.TemplateNotFoundException;
import com.thesis.workout.template.domain.model.Template;
import com.thesis.workout.template.domain.model.TemplateDay;
import com.thesis.workout.template.domain.model.TemplateDayExercise;
import com.thesis.workout.template.domain.model.TemplateVisibility;
import com.thesis.workout.template.infrastructure.repository.TemplateDayExerciseRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateDayRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the analyzer's read model with the marketplace access rules: a template is readable if
 * the caller owns it (private OK) or it is PUBLIC; otherwise 404 (no IDOR signal). Lives in the
 * template application layer so it can read template internals while the analyzer feature stays
 * decoupled from them.
 */
@Service
public class TemplateAnalysisSource {

    private final TemplateRepository templateRepository;
    private final TemplateDayRepository templateDayRepository;
    private final TemplateDayExerciseRepository templateDayExerciseRepository;

    public TemplateAnalysisSource(TemplateRepository templateRepository,
            TemplateDayRepository templateDayRepository,
            TemplateDayExerciseRepository templateDayExerciseRepository) {
        this.templateRepository = templateRepository;
        this.templateDayRepository = templateDayRepository;
        this.templateDayExerciseRepository = templateDayExerciseRepository;
    }

    @Transactional(readOnly = true)
    public TemplateAnalysisModel.Root loadForAnalysis(UUID userId, UUID templateId) {
        Template template = templateRepository.findByIdAndUserIdAndDeletedAtIsNull(templateId, userId)
                .or(() -> templateRepository.findByIdAndVisibilityAndDeletedAtIsNull(
                        templateId, TemplateVisibility.PUBLIC))
                .orElseThrow(TemplateNotFoundException::new);

        List<TemplateDay> days = templateDayRepository.findByTemplateIdOrderByDayNumberAsc(template.getId());
        List<UUID> dayIds = days.stream().map(TemplateDay::getId).toList();
        Map<UUID, List<TemplateDayExercise>> exercisesByDay = dayIds.isEmpty()
                ? Map.of()
                : templateDayExerciseRepository.findByTemplateDayIdInOrderByPositionAsc(dayIds).stream()
                        .collect(Collectors.groupingBy(TemplateDayExercise::getTemplateDayId));

        List<TemplateAnalysisModel.Day> modelDays = days.stream()
                .map(day -> new TemplateAnalysisModel.Day(
                        day.getId(), day.getDayNumber(), day.getName(), day.getFocus(),
                        exercisesByDay.getOrDefault(day.getId(), List.of()).stream()
                                .map(this::toExercise)
                                .toList()))
                .toList();

        return new TemplateAnalysisModel.Root(template.getId(), template.getName(), template.getSplitType(),
                template.getDifficulty(), template.getDaysPerWeek(), days.size(), modelDays);
    }

    private TemplateAnalysisModel.Exercise toExercise(TemplateDayExercise exercise) {
        List<TemplateAnalysisModel.Muscle> muscles = exercise.getMuscleGroups().stream()
                .map(group -> new TemplateAnalysisModel.Muscle(group.getMuscleGroupCode(), group.getRole()))
                .toList();
        return new TemplateAnalysisModel.Exercise(
                exercise.getExerciseNameSnapshot(), exercise.getExerciseTypeSnapshot(),
                exercise.getPlannedSets(), exercise.getPlannedReps(), exercise.getRestSeconds(), muscles);
    }
}
