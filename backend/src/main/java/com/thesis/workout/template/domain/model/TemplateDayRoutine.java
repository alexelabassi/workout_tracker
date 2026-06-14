package com.thesis.workout.template.domain.model;

import com.thesis.workout.routine.domain.model.RoutineType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A START/END routine attached to a template day. Like exercises, the routine reference is kept
 * (nullable) but name/content/type are snapshotted for historical truth. Position is unique per
 * (day, routine_type).
 */
@Entity
@Table(name = "template_day_routines")
public class TemplateDayRoutine {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "template_day_id", nullable = false)
    private UUID templateDayId;

    @Column(name = "routine_id")
    private UUID routineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "routine_type", nullable = false, length = 30)
    private RoutineType routineType;

    @Column(name = "routine_name_snapshot", nullable = false, length = 160)
    private String routineNameSnapshot;

    @Column(name = "routine_content_snapshot", nullable = false)
    private String routineContentSnapshot;

    @Column(name = "position", nullable = false)
    private int position;

    protected TemplateDayRoutine() {
    }

    public static TemplateDayRoutine createFor(UUID templateDayId, UUID routineId, RoutineType routineType,
            String routineNameSnapshot, String routineContentSnapshot, int position) {
        TemplateDayRoutine routine = new TemplateDayRoutine();
        routine.id = UUID.randomUUID();
        routine.templateDayId = templateDayId;
        routine.routineId = routineId;
        routine.routineType = routineType;
        routine.routineNameSnapshot = routineNameSnapshot;
        routine.routineContentSnapshot = routineContentSnapshot;
        routine.position = position;
        return routine;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateDayId() {
        return templateDayId;
    }

    public UUID getRoutineId() {
        return routineId;
    }

    public RoutineType getRoutineType() {
        return routineType;
    }

    public String getRoutineNameSnapshot() {
        return routineNameSnapshot;
    }

    public String getRoutineContentSnapshot() {
        return routineContentSnapshot;
    }

    public int getPosition() {
        return position;
    }
}
