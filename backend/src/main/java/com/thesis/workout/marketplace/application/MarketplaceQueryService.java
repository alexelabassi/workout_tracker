package com.thesis.workout.marketplace.application;

import com.thesis.workout.auth.domain.model.AppUser;
import com.thesis.workout.auth.infrastructure.repository.AppUserRepository;
import com.thesis.workout.marketplace.domain.model.TemplateVote;
import com.thesis.workout.marketplace.domain.model.TemplateVoteId;
import com.thesis.workout.marketplace.domain.model.VoteType;
import com.thesis.workout.marketplace.infrastructure.repository.MarketplaceTemplateRepository;
import com.thesis.workout.marketplace.infrastructure.repository.MarketplaceTemplateRepository.MarketplaceRow;
import com.thesis.workout.marketplace.infrastructure.repository.TemplateSaveRepository;
import com.thesis.workout.marketplace.infrastructure.repository.TemplateVoteRepository;
import com.thesis.workout.marketplace.web.dto.MarketplaceStatsResponse;
import com.thesis.workout.marketplace.web.dto.MarketplaceTemplateDetailResponse;
import com.thesis.workout.marketplace.web.dto.MarketplaceTemplateSummaryResponse;
import com.thesis.workout.shared.web.PagedResponse;
import com.thesis.workout.template.application.exception.TemplateNotFoundException;
import com.thesis.workout.template.application.TemplateService;
import com.thesis.workout.template.domain.model.Template;
import com.thesis.workout.template.domain.model.TemplateStats;
import com.thesis.workout.template.domain.model.TemplateVisibility;
import com.thesis.workout.template.infrastructure.repository.TemplateRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateStatsRepository;
import com.thesis.workout.template.web.dto.TemplateDetailResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Marketplace browse + public detail, with each result annotated with the viewer's vote/save state. */
@Service
public class MarketplaceQueryService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String FALLBACK_AUTHOR = "Community user";

    private final MarketplaceTemplateRepository marketplaceTemplateRepository;
    private final TemplateRepository templateRepository;
    private final TemplateStatsRepository templateStatsRepository;
    private final TemplateVoteRepository voteRepository;
    private final TemplateSaveRepository saveRepository;
    private final AppUserRepository appUserRepository;
    private final TemplateService templateService;

    public MarketplaceQueryService(MarketplaceTemplateRepository marketplaceTemplateRepository,
            TemplateRepository templateRepository, TemplateStatsRepository templateStatsRepository,
            TemplateVoteRepository voteRepository, TemplateSaveRepository saveRepository,
            AppUserRepository appUserRepository, TemplateService templateService) {
        this.marketplaceTemplateRepository = marketplaceTemplateRepository;
        this.templateRepository = templateRepository;
        this.templateStatsRepository = templateStatsRepository;
        this.voteRepository = voteRepository;
        this.saveRepository = saveRepository;
        this.appUserRepository = appUserRepository;
        this.templateService = templateService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<MarketplaceTemplateSummaryResponse> browse(UUID userId, String sort, String splitType,
            String difficulty, Integer daysPerWeek, boolean savedOnly, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = safePage * safeSize;

        List<MarketplaceRow> rows = switch (normalizeSort(sort)) {
            case "top" -> marketplaceTemplateRepository.browseTop(
                    userId, splitType, difficulty, daysPerWeek, savedOnly, safeSize, offset);
            case "trending" -> marketplaceTemplateRepository.browseTrending(
                    userId, splitType, difficulty, daysPerWeek, savedOnly, safeSize, offset);
            default -> marketplaceTemplateRepository.browseNewest(
                    userId, splitType, difficulty, daysPerWeek, savedOnly, safeSize, offset);
        };
        long total = marketplaceTemplateRepository.countPublic(userId, splitType, difficulty, daysPerWeek, savedOnly);

        List<UUID> ids = rows.stream().map(MarketplaceRow::getId).toList();
        Map<UUID, VoteType> myVotes = ids.isEmpty() ? Map.of()
                : voteRepository.findByUserAndTemplateIds(userId, ids).stream()
                        .collect(Collectors.toMap(v -> v.getId().getTemplateId(), TemplateVote::getVoteType));
        Set<UUID> saved = ids.isEmpty() ? Set.of()
                : Set.copyOf(saveRepository.findSavedTemplateIds(userId, ids));

        List<MarketplaceTemplateSummaryResponse> items = rows.stream()
                .map(row -> new MarketplaceTemplateSummaryResponse(
                        row.getId(), row.getName(), row.getDescription(), row.getSplitType(), row.getDifficulty(),
                        row.getDaysPerWeek(), row.getEstimatedDurationMinutes(), row.getPublishedAt(),
                        author(row.getAuthorDisplayName()),
                        new MarketplaceStatsResponse(row.getUpvotes(), row.getDownvotes(), row.getSaves(),
                                row.getUses(), row.getRatingScore()),
                        myVotes.get(row.getId()), saved.contains(row.getId())))
                .toList();

        boolean hasNext = (long) (safePage + 1) * safeSize < total;
        return new PagedResponse<>(items, safePage, safeSize, total, hasNext);
    }

    @Transactional(readOnly = true)
    public MarketplaceTemplateDetailResponse detail(UUID userId, UUID templateId) {
        TemplateDetailResponse template = templateService.getPublicDetail(templateId);
        Template owned = templateRepository
                .findByIdAndVisibilityAndDeletedAtIsNull(templateId, TemplateVisibility.PUBLIC)
                .orElseThrow(TemplateNotFoundException::new);
        String authorName = author(appUserRepository.findById(owned.getUserId())
                .map(AppUser::getDisplayName)
                .orElse(null));

        TemplateStats stats = templateStatsRepository.findById(templateId).orElseThrow(TemplateNotFoundException::new);
        MarketplaceStatsResponse statsResponse = new MarketplaceStatsResponse(
                stats.getUpvotesCount(), stats.getDownvotesCount(), stats.getSavesCount(),
                stats.getUsesCount(), stats.getRatingScore());
        VoteType myVote = voteRepository.findById(new TemplateVoteId(userId, templateId))
                .map(TemplateVote::getVoteType)
                .orElse(null);
        boolean saved = saveRepository.existsById(
                new com.thesis.workout.marketplace.domain.model.TemplateSaveId(userId, templateId));

        return new MarketplaceTemplateDetailResponse(template, authorName, statsResponse, myVote, saved);
    }

    private static String normalizeSort(String sort) {
        return sort == null ? "newest" : sort.trim().toLowerCase();
    }

    private static String author(String displayName) {
        return displayName == null || displayName.isBlank() ? FALLBACK_AUTHOR : displayName;
    }
}
