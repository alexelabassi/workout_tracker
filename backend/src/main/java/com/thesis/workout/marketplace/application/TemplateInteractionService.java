package com.thesis.workout.marketplace.application;

import com.thesis.workout.marketplace.application.exception.SelfVoteNotAllowedException;
import com.thesis.workout.marketplace.domain.model.TemplateSave;
import com.thesis.workout.marketplace.domain.model.TemplateSaveId;
import com.thesis.workout.marketplace.domain.model.TemplateVote;
import com.thesis.workout.marketplace.domain.model.TemplateVoteId;
import com.thesis.workout.marketplace.domain.model.VoteType;
import com.thesis.workout.marketplace.infrastructure.repository.TemplateSaveRepository;
import com.thesis.workout.marketplace.infrastructure.repository.TemplateVoteRepository;
import com.thesis.workout.marketplace.web.dto.MarketplaceInteractionResponse;
import com.thesis.workout.marketplace.web.dto.MarketplaceStatsResponse;
import com.thesis.workout.search.application.event.TemplateIndexEvent;
import com.thesis.workout.template.application.exception.TemplateNotFoundException;
import com.thesis.workout.template.domain.model.Template;
import com.thesis.workout.template.domain.model.TemplateStats;
import com.thesis.workout.template.domain.model.TemplateVisibility;
import com.thesis.workout.template.infrastructure.repository.TemplateRepository;
import com.thesis.workout.template.infrastructure.repository.TemplateStatsRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Voting and saving on PUBLIC templates. Counters are adjusted only when a row actually changes,
 * via atomic clamped SQL on {@code template_stats} (no read-modify-write race, no negative counts).
 * Self-voting is rejected; self-saving is allowed.
 */
@Service
public class TemplateInteractionService {

    private final TemplateRepository templateRepository;
    private final TemplateVoteRepository voteRepository;
    private final TemplateSaveRepository saveRepository;
    private final TemplateStatsRepository statsRepository;
    private final ApplicationEventPublisher events;

    public TemplateInteractionService(TemplateRepository templateRepository,
            TemplateVoteRepository voteRepository, TemplateSaveRepository saveRepository,
            TemplateStatsRepository statsRepository, ApplicationEventPublisher events) {
        this.templateRepository = templateRepository;
        this.voteRepository = voteRepository;
        this.saveRepository = saveRepository;
        this.statsRepository = statsRepository;
        this.events = events;
    }

    @Transactional
    public MarketplaceInteractionResponse vote(UUID userId, UUID templateId, VoteType voteType) {
        Template template = requirePublic(templateId);
        if (template.getUserId().equals(userId)) {
            throw new SelfVoteNotAllowedException();
        }
        TemplateVoteId id = new TemplateVoteId(userId, templateId);
        Optional<TemplateVote> existing = voteRepository.findById(id);

        if (existing.isEmpty()) {
            voteRepository.save(TemplateVote.of(userId, templateId, voteType));
            applyVote(templateId, voteType, +1);
        } else if (existing.get().getVoteType() == voteType) {
            voteRepository.delete(existing.get()); // same vote again → toggle off
            applyVote(templateId, voteType, -1);
        } else {
            existing.get().changeType(voteType); // opposite → switch
            applyVote(templateId, voteType, +1);
            applyVote(templateId, opposite(voteType), -1);
        }
        events.publishEvent(TemplateIndexEvent.stats(templateId));
        return state(templateId, userId);
    }

    @Transactional
    public MarketplaceInteractionResponse clearVote(UUID userId, UUID templateId) {
        requirePublic(templateId);
        voteRepository.findById(new TemplateVoteId(userId, templateId)).ifPresent(vote -> {
            voteRepository.delete(vote);
            applyVote(templateId, vote.getVoteType(), -1);
        });
        events.publishEvent(TemplateIndexEvent.stats(templateId));
        return state(templateId, userId);
    }

    @Transactional
    public MarketplaceInteractionResponse save(UUID userId, UUID templateId) {
        requirePublic(templateId);
        TemplateSaveId id = new TemplateSaveId(userId, templateId);
        if (!saveRepository.existsById(id)) {
            saveRepository.save(TemplateSave.of(userId, templateId));
            statsRepository.applySaveDelta(templateId, +1);
        }
        events.publishEvent(TemplateIndexEvent.stats(templateId));
        return state(templateId, userId);
    }

    @Transactional
    public MarketplaceInteractionResponse unsave(UUID userId, UUID templateId) {
        requirePublic(templateId);
        TemplateSaveId id = new TemplateSaveId(userId, templateId);
        if (saveRepository.existsById(id)) {
            saveRepository.deleteById(id);
            statsRepository.applySaveDelta(templateId, -1);
        }
        events.publishEvent(TemplateIndexEvent.stats(templateId));
        return state(templateId, userId);
    }

    private void applyVote(UUID templateId, VoteType type, int sign) {
        if (type == VoteType.UP) {
            statsRepository.applyVoteDelta(templateId, sign, 0);
        } else {
            statsRepository.applyVoteDelta(templateId, 0, sign);
        }
    }

    private static VoteType opposite(VoteType type) {
        return type == VoteType.UP ? VoteType.DOWN : VoteType.UP;
    }

    private Template requirePublic(UUID templateId) {
        return templateRepository.findByIdAndVisibilityAndDeletedAtIsNull(templateId, TemplateVisibility.PUBLIC)
                .orElseThrow(TemplateNotFoundException::new);
    }

    private MarketplaceInteractionResponse state(UUID templateId, UUID userId) {
        TemplateStats stats = statsRepository.findById(templateId).orElseThrow(TemplateNotFoundException::new);
        VoteType myVote = voteRepository.findById(new TemplateVoteId(userId, templateId))
                .map(TemplateVote::getVoteType)
                .orElse(null);
        boolean saved = saveRepository.existsById(new TemplateSaveId(userId, templateId));
        MarketplaceStatsResponse statsResponse = new MarketplaceStatsResponse(
                stats.getUpvotesCount(), stats.getDownvotesCount(), stats.getSavesCount(),
                stats.getUsesCount(), stats.getRatingScore());
        return new MarketplaceInteractionResponse(statsResponse, myVote, saved);
    }
}
