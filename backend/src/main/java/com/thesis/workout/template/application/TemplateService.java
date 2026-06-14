package com.thesis.workout.template.application;

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
import com.thesis.workout.template.web.dto.TemplateDayExerciseResponse;
import com.thesis.workout.template.web.dto.TemplateDayResponse;
import com.thesis.workout.template.web.dto.TemplateDayRoutineResponse;
import com.thesis.workout.template.web.dto.TemplateDetailResponse;
import com.thesis.workout.template.web.dto.TemplateSummaryResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Template CRUD plus assembly of the full template detail tree. All access is owner-scoped via
 * {@link TemplateAccess}. Creating a template also creates its zeroed {@code template_stats} row.
 * Templates are always PRIVATE in Phase 4.
 */
@Service
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateStatsRepository templateStatsRepository;
    private final TemplateDayRepository templateDayRepository;
    private final TemplateDayExerciseRepository templateDayExerciseRepository;
    private final TemplateDayRoutineRepository templateDayRoutineRepository;
    private final TemplateAccess templateAccess;

    public TemplateService(TemplateRepository templateRepository,
            TemplateStatsRepository templateStatsRepository,
            TemplateDayRepository templateDayRepository,
            TemplateDayExerciseRepository templateDayExerciseRepository,
            TemplateDayRoutineRepository templateDayRoutineRepository,
            TemplateAccess templateAccess) {
        this.templateRepository = templateRepository;
        this.templateStatsRepository = templateStatsRepository;
        this.templateDayRepository = templateDayRepository;
        this.templateDayExerciseRepository = templateDayExerciseRepository;
        this.templateDayRoutineRepository = templateDayRoutineRepository;
        this.templateAccess = templateAccess;
    }

    @Transactional(readOnly = true)
    public List<TemplateSummaryResponse> list(UUID userId) {
        return templateRepository.findByUserIdAndDeletedAtIsNullOrderByNameAsc(userId).stream()
                .map(template -> TemplateSummaryResponse.from(
                        template, templateDayRepository.countByTemplateId(template.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateDetailResponse get(UUID userId, UUID templateId) {
        Template template = templateAccess.requireOwnedTemplate(userId, templateId);
        return assembleDetail(template);
    }

    /** Assembles the detail tree for a PUBLIC template regardless of owner (marketplace read). */
    @Transactional(readOnly = true)
    public TemplateDetailResponse getPublicDetail(UUID templateId) {
        Template template = templateRepository
                .findByIdAndVisibilityAndDeletedAtIsNull(templateId, TemplateVisibility.PUBLIC)
                .orElseThrow(TemplateNotFoundException::new);
        return assembleDetail(template);
    }

    @Transactional
    public TemplateDetailResponse create(UUID userId, TemplateCommand command) {
        Template template = Template.createPrivate(
                userId,
                command.name().trim(),
                normalize(command.description()),
                command.splitType(),
                command.daysPerWeek(),
                command.difficulty(),
                command.estimatedDurationMinutes());
        Template saved = templateRepository.saveAndFlush(template);
        templateStatsRepository.save(TemplateStats.zeroFor(saved.getId()));
        return assembleDetail(saved);
    }

    @Transactional
    public TemplateDetailResponse update(UUID userId, UUID templateId, TemplateCommand command) {
        Template template = templateAccess.requireOwnedTemplate(userId, templateId);
        template.updateDetails(
                command.name().trim(),
                normalize(command.description()),
                command.splitType(),
                command.daysPerWeek(),
                command.difficulty(),
                command.estimatedDurationMinutes());
        return assembleDetail(templateRepository.saveAndFlush(template));
    }

    @Transactional
    public void delete(UUID userId, UUID templateId) {
        Template template = templateAccess.requireOwnedTemplate(userId, templateId);
        template.softDelete(Instant.now());
    }

    private TemplateDetailResponse assembleDetail(Template template) {
        List<TemplateDay> days = templateDayRepository.findByTemplateIdOrderByDayNumberAsc(template.getId());
        List<UUID> dayIds = days.stream().map(TemplateDay::getId).toList();

        Map<UUID, List<TemplateDayExerciseResponse>> exercisesByDay = dayIds.isEmpty()
                ? Map.of()
                : templateDayExerciseRepository.findByTemplateDayIdInOrderByPositionAsc(dayIds).stream()
                        .collect(Collectors.groupingBy(
                                TemplateDayExercise::getTemplateDayId,
                                Collectors.mapping(TemplateDayExerciseResponse::from, Collectors.toList())));

        Map<UUID, List<TemplateDayRoutineResponse>> routinesByDay = dayIds.isEmpty()
                ? Map.of()
                : templateDayRoutineRepository.findByTemplateDayIdInOrderByRoutineTypeAscPositionAsc(dayIds).stream()
                        .collect(Collectors.groupingBy(
                                TemplateDayRoutine::getTemplateDayId,
                                Collectors.mapping(TemplateDayRoutineResponse::from, Collectors.toList())));

        List<TemplateDayResponse> dayResponses = days.stream()
                .map(day -> TemplateDayResponse.from(
                        day,
                        exercisesByDay.getOrDefault(day.getId(), List.of()),
                        routinesByDay.getOrDefault(day.getId(), List.of())))
                .toList();

        List<String> muscleGroups = dayResponses.stream()
                .flatMap(day -> day.exercises().stream())
                .flatMap(exercise -> exercise.muscleGroups().stream())
                .map(group -> group.code())
                .distinct()
                .sorted()
                .toList();

        return TemplateDetailResponse.from(template, muscleGroups, dayResponses);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
