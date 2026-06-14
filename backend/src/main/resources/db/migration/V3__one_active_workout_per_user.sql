-- Final guarantee for "one active workout per user". The service pre-check
-- (existsByUserIdAndStatus) is only a friendly fast path: two concurrent start requests can both
-- pass it before either commits. This partial unique index makes the database the source of
-- truth, so at most one IN_PROGRESS session can exist per user. The losing insert raises a
-- unique violation, which the start handler maps to 409 ACTIVE_WORKOUT_EXISTS.
--
-- Note: workout_sets already has UNIQUE (session_exercise_id, set_number) from V1
-- (constraint unique_workout_set_number), so no set-number index is added here.
CREATE UNIQUE INDEX ux_workout_sessions_one_active_per_user
ON workout_sessions (user_id)
WHERE status = 'IN_PROGRESS';
