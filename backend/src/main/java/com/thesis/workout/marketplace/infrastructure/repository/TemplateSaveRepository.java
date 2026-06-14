package com.thesis.workout.marketplace.infrastructure.repository;

import com.thesis.workout.marketplace.domain.model.TemplateSave;
import com.thesis.workout.marketplace.domain.model.TemplateSaveId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateSaveRepository extends JpaRepository<TemplateSave, TemplateSaveId> {

    @Query("SELECT s.id.templateId FROM TemplateSave s WHERE s.id.userId = :userId AND s.id.templateId IN :templateIds")
    List<UUID> findSavedTemplateIds(@Param("userId") UUID userId,
            @Param("templateIds") Collection<UUID> templateIds);
}
