package com.thesis.workout.history.application;

import com.thesis.workout.history.infrastructure.repository.HistorySessionRepository;
import com.thesis.workout.history.infrastructure.repository.HistorySessionRepository.SessionSummaryRow;
import com.thesis.workout.history.web.dto.HistoryItemResponse;
import com.thesis.workout.shared.web.PagedResponse;
import com.thesis.workout.session.domain.model.WorkoutSession;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workout history list. Returns the caller's sessions of every status, newest first, each with
 * summary numbers. Duration is only known once a session is finished.
 */
@Service
public class HistoryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final HistorySessionRepository historySessionRepository;

    public HistoryService(HistorySessionRepository historySessionRepository) {
        this.historySessionRepository = historySessionRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<HistoryItemResponse> list(UUID userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "startedAt"));

        List<WorkoutSession> sessions = historySessionRepository.findByUserId(userId, pageable);
        long totalItems = historySessionRepository.countByUserId(userId);

        Map<UUID, SessionSummaryRow> summaries = sessions.isEmpty()
                ? Map.of()
                : historySessionRepository.summarize(sessions.stream().map(WorkoutSession::getId).toList()).stream()
                        .collect(Collectors.toMap(SessionSummaryRow::getSessionId, Function.identity()));

        List<HistoryItemResponse> items = sessions.stream()
                .map(session -> toItem(session, summaries.get(session.getId())))
                .toList();

        boolean hasNext = (long) (safePage + 1) * safeSize < totalItems;
        return new PagedResponse<>(items, safePage, safeSize, totalItems, hasNext);
    }

    private HistoryItemResponse toItem(WorkoutSession session, SessionSummaryRow summary) {
        Long durationSeconds = session.getFinishedAt() != null
                ? Duration.between(session.getStartedAt(), session.getFinishedAt()).toSeconds()
                : null;
        long exerciseCount = summary != null ? summary.getExerciseCount() : 0;
        long setCount = summary != null ? summary.getSetCount() : 0;
        BigDecimal totalVolume = summary != null ? summary.getTotalVolume() : BigDecimal.ZERO;
        return new HistoryItemResponse(
                session.getId(),
                session.getStatus(),
                session.getStartedAt(),
                session.getFinishedAt(),
                session.getTemplateNameSnapshot(),
                session.getTemplateDayNameSnapshot(),
                session.getGymNameSnapshot(),
                exerciseCount,
                setCount,
                totalVolume,
                durationSeconds);
    }
}
