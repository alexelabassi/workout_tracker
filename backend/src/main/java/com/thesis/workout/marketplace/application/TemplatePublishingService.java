package com.thesis.workout.marketplace.application;

import com.thesis.workout.marketplace.application.exception.TemplateNotPublishableException;
import com.thesis.workout.template.application.TemplateAccess;
import com.thesis.workout.template.application.TemplateService;
import com.thesis.workout.template.domain.model.Template;
import com.thesis.workout.template.domain.model.TemplateDay;
import com.thesis.workout.template.infrastructure.repository.TemplateDayExerciseRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateDayRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateRepository;
import com.thesis.workout.template.web.dto.TemplateDetailResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owner-only publishing of a template to the marketplace. Publishing requires the template to
 * have at least one day with at least one exercise. The DB CHECK ({@code PUBLIC ⇒ published_at})
 * is honored by {@code Template.publish}.
 */
@Service
public class TemplatePublishingService {

    private final TemplateAccess templateAccess;
    private final TemplateRepository templateRepository;
    private final TemplateDayRepository templateDayRepository;
    private final TemplateDayExerciseRepository templateDayExerciseRepository;
    private final TemplateService templateService;

    public TemplatePublishingService(TemplateAccess templateAccess, TemplateRepository templateRepository,
            TemplateDayRepository templateDayRepository,
            TemplateDayExerciseRepository templateDayExerciseRepository, TemplateService templateService) {
        this.templateAccess = templateAccess;
        this.templateRepository = templateRepository;
        this.templateDayRepository = templateDayRepository;
        this.templateDayExerciseRepository = templateDayExerciseRepository;
        this.templateService = templateService;
    }

    @Transactional
    public TemplateDetailResponse publish(UUID userId, UUID templateId) {
        Template template = templateAccess.requireOwnedTemplate(userId, templateId);
        requireNonEmpty(templateId);
        template.publish(Instant.now());
        templateRepository.saveAndFlush(template);
        return templateService.get(userId, templateId);
    }

    @Transactional
    public TemplateDetailResponse unpublish(UUID userId, UUID templateId) {
        Template template = templateAccess.requireOwnedTemplate(userId, templateId);
        template.unpublish();
        templateRepository.saveAndFlush(template);
        return templateService.get(userId, templateId);
    }

    private void requireNonEmpty(UUID templateId) {
        List<TemplateDay> days = templateDayRepository.findByTemplateIdOrderByDayNumberAsc(templateId);
        if (days.isEmpty()) {
            throw new TemplateNotPublishableException();
        }
        List<UUID> dayIds = days.stream().map(TemplateDay::getId).toList();
        if (templateDayExerciseRepository.findByTemplateDayIdInOrderByPositionAsc(dayIds).isEmpty()) {
            throw new TemplateNotPublishableException();
        }
    }
}
