package com.thesis.workout.marketplace.infrastructure.repository;

import com.thesis.workout.marketplace.domain.model.TemplateVote;
import com.thesis.workout.marketplace.domain.model.TemplateVoteId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateVoteRepository extends JpaRepository<TemplateVote, TemplateVoteId> {

    @Query("SELECT v FROM TemplateVote v WHERE v.id.userId = :userId AND v.id.templateId IN :templateIds")
    List<TemplateVote> findByUserAndTemplateIds(@Param("userId") UUID userId,
            @Param("templateIds") Collection<UUID> templateIds);
}
