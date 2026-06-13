-- V1__full_initial_schema.sql
-- PostgreSQL 16
-- Full initial schema for thesis-grade workout platform.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email varchar(320) NOT NULL,
    password_hash varchar(255) NOT NULL,
    display_name varchar(120),
    role varchar(30) NOT NULL DEFAULT 'USER',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT check_app_user_role CHECK (role IN ('USER', 'COACH', 'ADMIN'))
);

CREATE UNIQUE INDEX app_users_email_unique_idx ON app_users (lower(email));

CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token_hash varchar(255) NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX refresh_tokens_token_hash_unique_idx ON refresh_tokens (token_hash);
CREATE INDEX refresh_tokens_user_idx ON refresh_tokens (user_id);

CREATE TABLE coach_profiles (
    coach_user_id uuid PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    created_by_admin_id uuid REFERENCES app_users(id) ON DELETE SET NULL,
    bio text,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE coach_client_relationships (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    coach_user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    client_user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    status varchar(30) NOT NULL,
    created_by_user_id uuid REFERENCES app_users(id) ON DELETE SET NULL,
    accepted_at timestamptz,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT check_coach_client_status CHECK (status IN ('PENDING', 'ACTIVE', 'REVOKED', 'REJECTED')),
    CONSTRAINT check_coach_not_client CHECK (coach_user_id <> client_user_id)
);

CREATE INDEX coach_relationships_coach_idx ON coach_client_relationships (coach_user_id, status);
CREATE INDEX coach_relationships_client_idx ON coach_client_relationships (client_user_id, status);
CREATE UNIQUE INDEX coach_relationships_active_unique_idx
ON coach_client_relationships (coach_user_id, client_user_id)
WHERE status IN ('PENDING', 'ACTIVE');

CREATE TABLE muscle_groups (
    code varchar(50) PRIMARY KEY,
    display_name varchar(100) NOT NULL
);

INSERT INTO muscle_groups (code, display_name) VALUES
('CHEST', 'Chest'),
('BACK', 'Back'),
('SHOULDERS', 'Shoulders'),
('BICEPS', 'Biceps'),
('TRICEPS', 'Triceps'),
('QUADS', 'Quadriceps'),
('HAMSTRINGS', 'Hamstrings'),
('GLUTES', 'Glutes'),
('CALVES', 'Calves'),
('CORE', 'Core'),
('FOREARMS', 'Forearms'),
('TRAPS', 'Traps'),
('LATS', 'Lats'),
('FULL_BODY', 'Full Body'),
('CARDIO', 'Cardio')
ON CONFLICT (code) DO NOTHING;

CREATE TABLE exercises (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id uuid REFERENCES app_users(id) ON DELETE CASCADE,
    name varchar(160) NOT NULL,
    description text,
    exercise_type varchar(30) NOT NULL,
    visibility varchar(30) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT check_exercise_type CHECK (exercise_type IN ('STRENGTH', 'CARDIO', 'MOBILITY', 'OTHER')),
    CONSTRAINT check_exercise_visibility CHECK (visibility IN ('OFFICIAL', 'CUSTOM')),
    CONSTRAINT check_exercise_owner_visibility CHECK (
        (visibility = 'OFFICIAL' AND owner_user_id IS NULL)
        OR
        (visibility = 'CUSTOM' AND owner_user_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX unique_official_exercise_name_idx
ON exercises (lower(name))
WHERE deleted_at IS NULL AND owner_user_id IS NULL;

CREATE UNIQUE INDEX unique_custom_exercise_name_idx
ON exercises (owner_user_id, lower(name))
WHERE deleted_at IS NULL AND owner_user_id IS NOT NULL;

CREATE INDEX exercises_owner_idx ON exercises (owner_user_id);
CREATE INDEX exercises_visibility_idx ON exercises (visibility);

CREATE TABLE exercise_muscle_groups (
    exercise_id uuid NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    muscle_group_code varchar(50) NOT NULL REFERENCES muscle_groups(code),
    role varchar(30) NOT NULL,
    PRIMARY KEY (exercise_id, muscle_group_code),
    CONSTRAINT check_exercise_muscle_group_role CHECK (role IN ('PRIMARY', 'SECONDARY'))
);

CREATE INDEX exercise_muscle_groups_muscle_idx ON exercise_muscle_groups (muscle_group_code);

CREATE TABLE routines (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    name varchar(160) NOT NULL,
    routine_type varchar(30) NOT NULL,
    content text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT check_routine_type CHECK (routine_type IN ('START', 'END'))
);

CREATE UNIQUE INDEX routines_user_type_name_unique_idx
ON routines (user_id, routine_type, lower(name))
WHERE deleted_at IS NULL;

CREATE INDEX routines_user_idx ON routines (user_id);

CREATE TABLE gyms (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    name varchar(160) NOT NULL,
    location varchar(255),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX gyms_user_name_unique_idx
ON gyms (user_id, lower(name))
WHERE deleted_at IS NULL;

CREATE INDEX gyms_user_idx ON gyms (user_id);

CREATE TABLE equipment (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    gym_id uuid NOT NULL REFERENCES gyms(id) ON DELETE CASCADE,
    name varchar(160) NOT NULL,
    equipment_type varchar(50),
    notes text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT check_equipment_type CHECK (
        equipment_type IS NULL OR equipment_type IN (
            'BARBELL', 'DUMBBELL', 'MACHINE', 'CABLE', 'BENCH',
            'BODYWEIGHT', 'CARDIO_MACHINE', 'OTHER'
        )
    )
);

CREATE UNIQUE INDEX equipment_gym_name_unique_idx
ON equipment (gym_id, lower(name))
WHERE deleted_at IS NULL;

CREATE INDEX equipment_user_gym_idx ON equipment (user_id, gym_id);

CREATE TABLE workout_templates (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    name varchar(180) NOT NULL,
    description text,
    split_type varchar(40),
    days_per_week int,
    difficulty varchar(40),
    estimated_duration_minutes int,
    visibility varchar(30) NOT NULL DEFAULT 'PRIVATE',
    published_at timestamptz,
    copied_from_template_id uuid REFERENCES workout_templates(id) ON DELETE SET NULL,
    aggregated_muscle_groups varchar[] NOT NULL DEFAULT '{}'::varchar[],
    aggregated_official_exercise_ids uuid[] NOT NULL DEFAULT '{}'::uuid[],
    aggregated_exercise_names text[] NOT NULL DEFAULT '{}'::text[],
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT check_template_split_type CHECK (
        split_type IS NULL OR split_type IN ('FULL_BODY', 'UPPER_LOWER', 'PPL', 'BRO_SPLIT', 'CUSTOM')
    ),
    CONSTRAINT check_template_days_per_week CHECK (days_per_week IS NULL OR days_per_week BETWEEN 1 AND 7),
    CONSTRAINT check_template_difficulty CHECK (
        difficulty IS NULL OR difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')
    ),
    CONSTRAINT check_template_duration CHECK (
        estimated_duration_minutes IS NULL OR estimated_duration_minutes > 0
    ),
    CONSTRAINT check_template_visibility CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    CONSTRAINT check_template_published_visibility CHECK (
        (visibility = 'PUBLIC' AND published_at IS NOT NULL)
        OR
        (visibility = 'PRIVATE')
    )
);

CREATE INDEX templates_user_idx ON workout_templates (user_id);
CREATE INDEX templates_public_idx ON workout_templates (visibility, published_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX templates_filter_idx ON workout_templates (split_type, days_per_week, difficulty) WHERE deleted_at IS NULL;
CREATE INDEX workout_templates_agg_muscles_gin_idx ON workout_templates USING gin (aggregated_muscle_groups);
CREATE INDEX workout_templates_agg_official_exercises_gin_idx ON workout_templates USING gin (aggregated_official_exercise_ids);
CREATE INDEX workout_templates_agg_exercise_names_gin_idx ON workout_templates USING gin (aggregated_exercise_names);

CREATE TABLE template_stats (
    template_id uuid PRIMARY KEY REFERENCES workout_templates(id) ON DELETE CASCADE,
    upvotes_count int NOT NULL DEFAULT 0,
    downvotes_count int NOT NULL DEFAULT 0,
    saves_count int NOT NULL DEFAULT 0,
    uses_count int NOT NULL DEFAULT 0,
    rating_score numeric(12,4) NOT NULL DEFAULT 0,
    trending_score numeric(12,4) NOT NULL DEFAULT 0,
    lock_version int NOT NULL DEFAULT 0,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT check_template_stats_non_negative CHECK (
        upvotes_count >= 0 AND downvotes_count >= 0 AND saves_count >= 0
        AND uses_count >= 0 AND lock_version >= 0
    )
);

CREATE TABLE template_days (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id uuid NOT NULL REFERENCES workout_templates(id) ON DELETE CASCADE,
    day_number int NOT NULL,
    name varchar(160) NOT NULL,
    focus varchar(40),
    estimated_duration_minutes int,
    notes text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT check_template_day_number CHECK (day_number > 0),
    CONSTRAINT check_template_day_focus CHECK (
        focus IS NULL OR focus IN ('UPPER', 'LOWER', 'PUSH', 'PULL', 'LEGS', 'FULL_BODY', 'CUSTOM')
    ),
    CONSTRAINT check_template_day_duration CHECK (
        estimated_duration_minutes IS NULL OR estimated_duration_minutes > 0
    ),
    CONSTRAINT unique_template_day_number UNIQUE (template_id, day_number)
);

CREATE INDEX template_days_template_idx ON template_days (template_id);

CREATE TABLE template_day_exercises (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    template_day_id uuid NOT NULL REFERENCES template_days(id) ON DELETE CASCADE,
    exercise_id uuid REFERENCES exercises(id) ON DELETE SET NULL,
    exercise_name_snapshot varchar(160) NOT NULL,
    exercise_type_snapshot varchar(30) NOT NULL,
    position int NOT NULL,
    planned_sets int,
    planned_reps varchar(50),
    planned_weight numeric(8,2),
    rest_seconds int,
    note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT check_template_day_exercise_type_snapshot CHECK (
        exercise_type_snapshot IN ('STRENGTH', 'CARDIO', 'MOBILITY', 'OTHER')
    ),
    CONSTRAINT check_template_day_exercise_position CHECK (position > 0),
    CONSTRAINT check_template_day_exercise_metrics CHECK (
        (planned_sets IS NULL OR planned_sets > 0)
        AND (planned_weight IS NULL OR planned_weight >= 0)
        AND (rest_seconds IS NULL OR rest_seconds >= 0)
    ),
    CONSTRAINT unique_template_day_exercise_position UNIQUE (template_day_id, position)
);

CREATE INDEX template_day_exercises_day_idx ON template_day_exercises (template_day_id);
CREATE INDEX template_day_exercises_exercise_idx ON template_day_exercises (exercise_id);

CREATE TABLE template_day_exercise_muscle_groups (
    template_day_exercise_id uuid NOT NULL REFERENCES template_day_exercises(id) ON DELETE CASCADE,
    muscle_group_code varchar(50) NOT NULL,
    role varchar(30) NOT NULL,
    PRIMARY KEY (template_day_exercise_id, muscle_group_code),
    CONSTRAINT check_template_day_exercise_muscle_role CHECK (role IN ('PRIMARY', 'SECONDARY'))
);

CREATE INDEX template_day_exercise_muscles_code_idx ON template_day_exercise_muscle_groups (muscle_group_code);

CREATE TABLE template_day_routines (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    template_day_id uuid NOT NULL REFERENCES template_days(id) ON DELETE CASCADE,
    routine_id uuid REFERENCES routines(id) ON DELETE SET NULL,
    routine_type varchar(30) NOT NULL,
    routine_name_snapshot varchar(160) NOT NULL,
    routine_content_snapshot text NOT NULL,
    position int NOT NULL,
    CONSTRAINT check_template_day_routine_type CHECK (routine_type IN ('START', 'END')),
    CONSTRAINT check_template_day_routine_position CHECK (position > 0),
    CONSTRAINT unique_template_day_routine_position UNIQUE (template_day_id, routine_type, position)
);

CREATE INDEX template_day_routines_day_idx ON template_day_routines (template_day_id);

CREATE TABLE workout_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    template_id uuid REFERENCES workout_templates(id) ON DELETE SET NULL,
    template_day_id uuid REFERENCES template_days(id) ON DELETE SET NULL,
    template_name_snapshot varchar(180),
    template_day_name_snapshot varchar(160),
    gym_id uuid REFERENCES gyms(id) ON DELETE SET NULL,
    gym_name_snapshot varchar(160),
    status varchar(30) NOT NULL,
    started_at timestamptz NOT NULL DEFAULT now(),
    finished_at timestamptz,
    notes text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT check_workout_session_status CHECK (status IN ('IN_PROGRESS', 'FINISHED', 'CANCELLED')),
    CONSTRAINT check_workout_session_times CHECK (finished_at IS NULL OR finished_at >= started_at)
);

CREATE INDEX sessions_user_started_idx ON workout_sessions (user_id, started_at DESC);
CREATE INDEX sessions_status_idx ON workout_sessions (status);

CREATE TABLE session_exercises (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id uuid NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    original_template_day_exercise_id uuid REFERENCES template_day_exercises(id) ON DELETE SET NULL,
    original_exercise_id uuid REFERENCES exercises(id) ON DELETE SET NULL,
    exercise_name_snapshot varchar(160) NOT NULL,
    exercise_type_snapshot varchar(30) NOT NULL,
    position int NOT NULL,
    planned_sets_snapshot int,
    planned_reps_snapshot varchar(50),
    planned_weight_snapshot numeric(8,2),
    rest_seconds_snapshot int,
    template_note_snapshot text,
    is_extra_exercise boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT check_session_exercise_type_snapshot CHECK (
        exercise_type_snapshot IN ('STRENGTH', 'CARDIO', 'MOBILITY', 'OTHER')
    ),
    CONSTRAINT check_session_exercise_position CHECK (position > 0),
    CONSTRAINT check_session_exercise_metrics CHECK (
        (planned_sets_snapshot IS NULL OR planned_sets_snapshot > 0)
        AND (planned_weight_snapshot IS NULL OR planned_weight_snapshot >= 0)
        AND (rest_seconds_snapshot IS NULL OR rest_seconds_snapshot >= 0)
    )
);

CREATE INDEX session_exercises_position_idx ON session_exercises (session_id, position);
CREATE INDEX session_exercises_session_idx ON session_exercises (session_id);

CREATE TABLE session_exercise_muscle_groups (
    session_exercise_id uuid NOT NULL REFERENCES session_exercises(id) ON DELETE CASCADE,
    muscle_group_code_snapshot varchar(50) NOT NULL,
    role_snapshot varchar(30) NOT NULL,
    PRIMARY KEY (session_exercise_id, muscle_group_code_snapshot),
    CONSTRAINT check_session_exercise_muscle_role CHECK (role_snapshot IN ('PRIMARY', 'SECONDARY'))
);

CREATE INDEX session_exercise_muscles_code_idx ON session_exercise_muscle_groups (muscle_group_code_snapshot);

CREATE TABLE session_routines (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id uuid NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    original_routine_id uuid REFERENCES routines(id) ON DELETE SET NULL,
    routine_type varchar(30) NOT NULL,
    routine_name_snapshot varchar(160) NOT NULL,
    routine_content_snapshot text NOT NULL,
    position int NOT NULL,
    started_at timestamptz,
    ended_at timestamptz,
    CONSTRAINT check_session_routine_type CHECK (routine_type IN ('START', 'END')),
    CONSTRAINT check_session_routine_position CHECK (position > 0),
    CONSTRAINT check_session_routine_times CHECK (
        ended_at IS NULL OR started_at IS NULL OR ended_at >= started_at
    )
);

CREATE INDEX session_routines_session_idx ON session_routines (session_id);

CREATE TABLE workout_sets (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_exercise_id uuid NOT NULL REFERENCES session_exercises(id) ON DELETE CASCADE,
    set_number int NOT NULL,
    set_type varchar(30) NOT NULL DEFAULT 'WORKING',
    weight numeric(8,2),
    reps int,
    duration_seconds int,
    distance_meters numeric(10,2),
    rpe numeric(3,1),
    note text,
    equipment_id uuid REFERENCES equipment(id) ON DELETE SET NULL,
    equipment_name_snapshot varchar(160),
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT check_workout_set_number CHECK (set_number > 0),
    CONSTRAINT check_workout_set_type CHECK (set_type IN ('WARMUP', 'WORKING', 'DROP', 'FAILURE')),
    CONSTRAINT check_workout_set_metrics CHECK (
        (weight IS NULL OR weight >= 0)
        AND (reps IS NULL OR reps >= 0)
        AND (duration_seconds IS NULL OR duration_seconds >= 0)
        AND (distance_meters IS NULL OR distance_meters >= 0)
        AND (rpe IS NULL OR rpe BETWEEN 1.0 AND 10.0)
    ),
    CONSTRAINT unique_workout_set_number UNIQUE (session_exercise_id, set_number)
);

CREATE INDEX workout_sets_exercise_idx ON workout_sets (session_exercise_id);
CREATE INDEX workout_sets_completed_idx ON workout_sets (completed_at);

CREATE TABLE coach_session_comments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    coach_user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    client_user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    session_id uuid NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    comment text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE INDEX coach_session_comments_session_idx ON coach_session_comments (session_id);
CREATE INDEX coach_session_comments_client_idx ON coach_session_comments (client_user_id);

CREATE TABLE template_votes (
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    template_id uuid NOT NULL REFERENCES workout_templates(id) ON DELETE CASCADE,
    vote_type varchar(30) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, template_id),
    CONSTRAINT check_template_vote_type CHECK (vote_type IN ('UP', 'DOWN'))
);

CREATE INDEX template_votes_template_idx ON template_votes (template_id);

CREATE TABLE template_saves (
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    template_id uuid NOT NULL REFERENCES workout_templates(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, template_id)
);

CREATE INDEX template_saves_template_idx ON template_saves (template_id);

CREATE TABLE template_use_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    source_template_id uuid REFERENCES workout_templates(id) ON DELETE SET NULL,
    copied_template_id uuid REFERENCES workout_templates(id) ON DELETE SET NULL,
    source_template_name_snapshot varchar(180) NOT NULL,
    copied_template_name_snapshot varchar(180) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX template_use_events_user_idx ON template_use_events (user_id);
CREATE INDEX template_use_events_source_idx ON template_use_events (source_template_id);
CREATE INDEX template_use_events_copied_idx ON template_use_events (copied_template_id);

CREATE TABLE coach_template_assignments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    coach_user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    client_user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    template_id uuid REFERENCES workout_templates(id) ON DELETE SET NULL,
    note text,
    status varchar(30) NOT NULL DEFAULT 'ASSIGNED',
    assigned_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    revoked_at timestamptz,
    CONSTRAINT check_coach_assignment_status CHECK (status IN ('ASSIGNED', 'COMPLETED', 'REVOKED'))
);

CREATE INDEX coach_template_assignments_coach_idx ON coach_template_assignments (coach_user_id, status);
CREATE INDEX coach_template_assignments_client_idx ON coach_template_assignments (client_user_id, status);

CREATE TABLE template_analysis_results (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id uuid NOT NULL REFERENCES workout_templates(id) ON DELETE CASCADE,
    score int NOT NULL,
    category varchar(50) NOT NULL,
    positives jsonb NOT NULL DEFAULT '[]'::jsonb,
    warnings jsonb NOT NULL DEFAULT '[]'::jsonb,
    notes jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT check_template_analysis_score CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT check_template_analysis_category CHECK (
        category IN ('NEEDS_REVIEW', 'DECENT_STRUCTURE', 'WELL_STRUCTURED')
    )
);

CREATE INDEX template_analysis_results_template_idx ON template_analysis_results (template_id, created_at DESC);

CREATE TABLE outbox_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type varchar(100) NOT NULL,
    aggregate_id uuid NOT NULL,
    event_type varchar(100) NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(30) NOT NULL DEFAULT 'PENDING',
    attempts int NOT NULL DEFAULT 0,
    available_at timestamptz NOT NULL DEFAULT now(),
    processed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    last_error text,
    CONSTRAINT check_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED')),
    CONSTRAINT check_outbox_attempts CHECK (attempts >= 0)
);

CREATE INDEX outbox_events_processing_idx
ON outbox_events (status, available_at, created_at);

CREATE INDEX outbox_events_aggregate_idx
ON outbox_events (aggregate_type, aggregate_id);
