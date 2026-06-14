-- Seed data only: curated official exercises (owner_user_id NULL, visibility OFFICIAL) and
-- their muscle-group mappings. No schema changes. Fixed UUIDs keep the muscle-group join rows
-- readable and stable. ON CONFLICT guards make the migration safe if rerun against existing rows.

INSERT INTO exercises (id, owner_user_id, name, description, exercise_type, visibility) VALUES
('00000000-0000-0000-0000-000000000101', NULL, 'Barbell Back Squat', 'Compound lower-body lift with the barbell racked on the upper back.', 'STRENGTH', 'OFFICIAL'),
('00000000-0000-0000-0000-000000000102', NULL, 'Barbell Deadlift', 'Full-body hip hinge lifting a loaded barbell from the floor.', 'STRENGTH', 'OFFICIAL'),
('00000000-0000-0000-0000-000000000103', NULL, 'Barbell Bench Press', 'Horizontal pressing movement performed lying on a flat bench.', 'STRENGTH', 'OFFICIAL'),
('00000000-0000-0000-0000-000000000104', NULL, 'Overhead Press', 'Standing vertical press of a barbell from the shoulders to overhead.', 'STRENGTH', 'OFFICIAL'),
('00000000-0000-0000-0000-000000000105', NULL, 'Pull-Up', 'Bodyweight vertical pull from a dead hang to chin over the bar.', 'STRENGTH', 'OFFICIAL'),
('00000000-0000-0000-0000-000000000106', NULL, 'Barbell Bent-Over Row', 'Horizontal pull of a barbell toward the torso while hinged forward.', 'STRENGTH', 'OFFICIAL'),
('00000000-0000-0000-0000-000000000107', NULL, 'Romanian Deadlift', 'Hip-hinge emphasizing the posterior chain with a controlled eccentric.', 'STRENGTH', 'OFFICIAL'),
('00000000-0000-0000-0000-000000000108', NULL, 'Dumbbell Walking Lunge', 'Alternating forward lunges carrying a dumbbell in each hand.', 'STRENGTH', 'OFFICIAL'),
('00000000-0000-0000-0000-000000000109', NULL, 'Lat Pulldown', 'Cable vertical pull bringing the bar to the upper chest.', 'STRENGTH', 'OFFICIAL'),
('00000000-0000-0000-0000-000000000110', NULL, 'Barbell Biceps Curl', 'Elbow flexion curling a barbell from the thighs to the shoulders.', 'STRENGTH', 'OFFICIAL'),
('00000000-0000-0000-0000-000000000111', NULL, 'Triceps Pushdown', 'Cable elbow extension pushing the attachment down to lockout.', 'STRENGTH', 'OFFICIAL'),
('00000000-0000-0000-0000-000000000112', NULL, 'Standing Calf Raise', 'Plantarflexion raising the heels against load through full range.', 'STRENGTH', 'OFFICIAL'),
('00000000-0000-0000-0000-000000000113', NULL, 'Plank', 'Isometric core hold maintaining a straight line from head to heels.', 'OTHER', 'OFFICIAL'),
('00000000-0000-0000-0000-000000000114', NULL, 'Treadmill Run', 'Steady-state or interval running performed on a treadmill.', 'CARDIO', 'OFFICIAL')
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_muscle_groups (exercise_id, muscle_group_code, role) VALUES
-- Barbell Back Squat
('00000000-0000-0000-0000-000000000101', 'QUADS', 'PRIMARY'),
('00000000-0000-0000-0000-000000000101', 'GLUTES', 'SECONDARY'),
('00000000-0000-0000-0000-000000000101', 'HAMSTRINGS', 'SECONDARY'),
('00000000-0000-0000-0000-000000000101', 'CORE', 'SECONDARY'),
-- Barbell Deadlift
('00000000-0000-0000-0000-000000000102', 'HAMSTRINGS', 'PRIMARY'),
('00000000-0000-0000-0000-000000000102', 'GLUTES', 'PRIMARY'),
('00000000-0000-0000-0000-000000000102', 'BACK', 'SECONDARY'),
('00000000-0000-0000-0000-000000000102', 'LATS', 'SECONDARY'),
('00000000-0000-0000-0000-000000000102', 'FOREARMS', 'SECONDARY'),
('00000000-0000-0000-0000-000000000102', 'CORE', 'SECONDARY'),
-- Barbell Bench Press
('00000000-0000-0000-0000-000000000103', 'CHEST', 'PRIMARY'),
('00000000-0000-0000-0000-000000000103', 'TRICEPS', 'SECONDARY'),
('00000000-0000-0000-0000-000000000103', 'SHOULDERS', 'SECONDARY'),
-- Overhead Press
('00000000-0000-0000-0000-000000000104', 'SHOULDERS', 'PRIMARY'),
('00000000-0000-0000-0000-000000000104', 'TRICEPS', 'SECONDARY'),
('00000000-0000-0000-0000-000000000104', 'CORE', 'SECONDARY'),
-- Pull-Up
('00000000-0000-0000-0000-000000000105', 'LATS', 'PRIMARY'),
('00000000-0000-0000-0000-000000000105', 'BACK', 'SECONDARY'),
('00000000-0000-0000-0000-000000000105', 'BICEPS', 'SECONDARY'),
('00000000-0000-0000-0000-000000000105', 'FOREARMS', 'SECONDARY'),
-- Barbell Bent-Over Row
('00000000-0000-0000-0000-000000000106', 'BACK', 'PRIMARY'),
('00000000-0000-0000-0000-000000000106', 'LATS', 'SECONDARY'),
('00000000-0000-0000-0000-000000000106', 'BICEPS', 'SECONDARY'),
-- Romanian Deadlift
('00000000-0000-0000-0000-000000000107', 'HAMSTRINGS', 'PRIMARY'),
('00000000-0000-0000-0000-000000000107', 'GLUTES', 'SECONDARY'),
('00000000-0000-0000-0000-000000000107', 'BACK', 'SECONDARY'),
-- Dumbbell Walking Lunge
('00000000-0000-0000-0000-000000000108', 'QUADS', 'PRIMARY'),
('00000000-0000-0000-0000-000000000108', 'GLUTES', 'SECONDARY'),
('00000000-0000-0000-0000-000000000108', 'HAMSTRINGS', 'SECONDARY'),
-- Lat Pulldown
('00000000-0000-0000-0000-000000000109', 'LATS', 'PRIMARY'),
('00000000-0000-0000-0000-000000000109', 'BACK', 'SECONDARY'),
('00000000-0000-0000-0000-000000000109', 'BICEPS', 'SECONDARY'),
-- Barbell Biceps Curl
('00000000-0000-0000-0000-000000000110', 'BICEPS', 'PRIMARY'),
('00000000-0000-0000-0000-000000000110', 'FOREARMS', 'SECONDARY'),
-- Triceps Pushdown
('00000000-0000-0000-0000-000000000111', 'TRICEPS', 'PRIMARY'),
-- Standing Calf Raise
('00000000-0000-0000-0000-000000000112', 'CALVES', 'PRIMARY'),
-- Plank
('00000000-0000-0000-0000-000000000113', 'CORE', 'PRIMARY'),
-- Treadmill Run
('00000000-0000-0000-0000-000000000114', 'CARDIO', 'PRIMARY'),
('00000000-0000-0000-0000-000000000114', 'QUADS', 'SECONDARY'),
('00000000-0000-0000-0000-000000000114', 'CALVES', 'SECONDARY')
ON CONFLICT (exercise_id, muscle_group_code) DO NOTHING;
