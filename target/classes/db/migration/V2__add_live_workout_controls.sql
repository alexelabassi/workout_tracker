ALTER TABLE session_routine_snapshots
    ADD COLUMN started_at TIMESTAMP;

ALTER TABLE session_routine_snapshots
    ADD COLUMN ended_at TIMESTAMP;

ALTER TABLE session_exercise_snapshots
    ALTER COLUMN source_template_exercise_id DROP NOT NULL;
