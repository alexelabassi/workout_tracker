package com.thesis.workout.search.application;

import com.thesis.workout.analyzer.application.TemplateAnalyzerService;
import com.thesis.workout.analyzer.web.dto.AnalysisResponse;
import com.thesis.workout.analyzer.web.dto.AnalysisWarning;
import com.thesis.workout.analyzer.web.dto.MuscleFrequencyResponse;
import com.thesis.workout.search.application.document.TemplateDocument;
import com.thesis.workout.template.domain.model.Template;
import com.thesis.workout.template.domain.model.TemplateDay;
import com.thesis.workout.template.domain.model.TemplateDayExercise;
import com.thesis.workout.template.domain.model.TemplateDayExerciseMuscleGroup;
import com.thesis.workout.template.domain.model.TemplateDayRoutine;
import com.thesis.workout.template.domain.model.TemplateStats;
import com.thesis.workout.template.infrastructure.repository.TemplateDayExerciseRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateDayRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateDayRoutineRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateStatsRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds a denormalised {@link TemplateDocument} from the authoritative PostgreSQL state. Runs in a
 * read-only transaction (invoked across a bean boundary so the Spring proxy applies) so the lazy
 * muscle-group snapshots load. The analyzer-derived structural fields are computed here, so they are
 * refreshed only on a structural reindex — a vote/save/use never recomputes them (see
 * {@code TemplateIndexer.indexStatsOnly}).
 */
@Service
public class TemplateDocumentAssembler {

    private static final Set<String> MAJOR_MUSCLES =
            Set.of("CHEST", "BACK", "SHOULDERS", "QUADS", "HAMSTRINGS", "GLUTES");

    private final TemplateRepository templateRepository;
    private final TemplateDayRepository templateDayRepository;
    private final TemplateDayExerciseRepository templateDayExerciseRepository;
    private final TemplateDayRoutineRepository templateDayRoutineRepository;
    private final TemplateStatsRepository templateStatsRepository;
    private final TemplateAnalyzerService analyzerService;

    public TemplateDocumentAssembler(TemplateRepository templateRepository,
            TemplateDayRepository templateDayRepository,
            TemplateDayExerciseRepository templateDayExerciseRepository,
            TemplateDayRoutineRepository templateDayRoutineRepository,
            TemplateStatsRepository templateStatsRepository,
            TemplateAnalyzerService analyzerService) {
        this.templateRepository = templateRepository;
        this.templateDayRepository = templateDayRepository;
        this.templateDayExerciseRepository = templateDayExerciseRepository;
        this.templateDayRoutineRepository = templateDayRoutineRepository;
        this.templateStatsRepository = templateStatsRepository;
        this.analyzerService = analyzerService;
    }

    @Transactional(readOnly = true)
    public Optional<TemplateDocument> assemble(UUID templateId) {
        Optional<Template> maybe = templateRepository.findById(templateId);
        if (maybe.isEmpty() || maybe.get().getDeletedAt() != null) {
            return Optional.empty();
        }
        Template template = maybe.get();

        List<TemplateDay> days = templateDayRepository.findByTemplateIdOrderByDayNumberAsc(templateId);
        List<UUID> dayIds = days.stream().map(TemplateDay::getId).toList();
        List<TemplateDayExercise> exercises = dayIds.isEmpty() ? List.of()
                : templateDayExerciseRepository.findByTemplateDayIdInOrderByPositionAsc(dayIds);
        List<TemplateDayRoutine> routines = dayIds.isEmpty() ? List.of()
                : templateDayRoutineRepository.findByTemplateDayIdInOrderByRoutineTypeAscPositionAsc(dayIds);

        List<String> exerciseNames = exercises.stream()
                .map(TemplateDayExercise::getExerciseNameSnapshot)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<String> muscleGroups = exercises.stream()
                .flatMap(e -> e.getMuscleGroups().stream())
                .map(TemplateDayExerciseMuscleGroup::getMuscleGroupCode)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        List<String> dayNames = days.stream().map(TemplateDay::getName).filter(Objects::nonNull).toList();
        List<String> dayFocuses = days.stream()
                .map(TemplateDay::getFocus)
                .filter(Objects::nonNull)
                .map(Enum::name)
                .distinct()
                .toList();
        List<String> routineNames = routines.stream()
                .map(TemplateDayRoutine::getRoutineNameSnapshot)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<String> routineContents = routines.stream()
                .map(TemplateDayRoutine::getRoutineContentSnapshot)
                .filter(Objects::nonNull)
                .toList();

        TemplateStats stats = templateStatsRepository.findById(templateId).orElse(null);

        AnalyzerFields analyzer = analyze(template);

        return Optional.of(new TemplateDocument(
                template.getId().toString(),
                template.getUserId().toString(),
                template.getVisibility().name(),
                template.getName(),
                template.getDescription(),
                template.getSplitType() != null ? template.getSplitType().name() : null,
                template.getDifficulty() != null ? template.getDifficulty().name() : null,
                template.getDaysPerWeek(),
                template.getEstimatedDurationMinutes(),
                epochMillis(template.getPublishedAt()),
                epochMillis(template.getCreatedAt()),
                template.getCopiedFromTemplateId() != null ? template.getCopiedFromTemplateId().toString() : null,
                exerciseNames,
                muscleGroups,
                String.join(" ", muscleGroups),
                dayNames,
                dayFocuses,
                routineNames,
                routineContents,
                stats != null ? stats.getRatingScore().doubleValue() : 0.0,
                stats != null ? stats.getUpvotesCount() : 0,
                stats != null ? stats.getDownvotesCount() : 0,
                stats != null ? stats.getSavesCount() : 0,
                stats != null ? stats.getUsesCount() : 0,
                analyzer.score(),
                analyzer.category(),
                analyzer.warningCodes(),
                analyzer.missingMajorMuscles()));
    }

    /** Stat counters only — used by the partial (non-structural) update path. */
    @Transactional(readOnly = true)
    public Optional<TemplateStats> loadStats(UUID templateId) {
        return templateStatsRepository.findById(templateId);
    }

    private AnalyzerFields analyze(Template template) {
        try {
            AnalysisResponse analysis = analyzerService.analyze(template.getUserId(), template.getId());
            List<String> warningCodes = analysis.warnings().stream()
                    .map(AnalysisWarning::code)
                    .distinct()
                    .toList();
            List<String> missingMajors = analysis.frequencyByMuscleGroup().stream()
                    .filter(f -> MAJOR_MUSCLES.contains(f.muscleGroup()) && f.daysPerWeek() == 0)
                    .map(MuscleFrequencyResponse::muscleGroup)
                    .toList();
            return new AnalyzerFields(analysis.overallScore(), analysis.category(), warningCodes, missingMajors);
        } catch (RuntimeException ex) {
            // Analyzer enrichment is best-effort; a template with no days still indexes (searchable),
            // it just carries no structural score until it has analyzable content.
            return new AnalyzerFields(null, null, List.of(), List.of());
        }
    }

    private static Long epochMillis(Instant instant) {
        return instant != null ? instant.toEpochMilli() : null;
    }

    private record AnalyzerFields(Integer score, String category, List<String> warningCodes,
            List<String> missingMajorMuscles) {
    }
}
