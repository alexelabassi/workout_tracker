package com.thesis.workout.demo;

import com.thesis.workout.auth.domain.model.AppUser;
import com.thesis.workout.auth.domain.model.Role;
import com.thesis.workout.auth.infrastructure.repository.AppUserRepository;
import com.thesis.workout.exercise.application.CustomExerciseCommand;
import com.thesis.workout.exercise.application.ExerciseService;
import com.thesis.workout.exercise.application.MuscleAssignment;
import com.thesis.workout.exercise.domain.model.ExerciseType;
import com.thesis.workout.exercise.domain.model.MuscleRole;
import com.thesis.workout.gym.application.EquipmentCommand;
import com.thesis.workout.gym.application.EquipmentService;
import com.thesis.workout.gym.application.GymCommand;
import com.thesis.workout.gym.application.GymService;
import com.thesis.workout.gym.domain.model.EquipmentType;
import com.thesis.workout.coaching.domain.model.CoachClientRelationship;
import com.thesis.workout.coaching.domain.model.CoachProfile;
import com.thesis.workout.coaching.infrastructure.repository.CoachClientRelationshipRepository;
import com.thesis.workout.coaching.infrastructure.repository.CoachProfileRepository;
import com.thesis.workout.routine.application.RoutineCommand;
import com.thesis.workout.routine.application.RoutineService;
import com.thesis.workout.routine.domain.model.RoutineType;
import com.thesis.workout.search.application.SearchRebuildService;
import com.thesis.workout.session.application.SetCommand;
import com.thesis.workout.session.application.WorkoutSessionService;
import com.thesis.workout.session.application.WorkoutSetService;
import com.thesis.workout.session.domain.model.SetType;
import com.thesis.workout.session.web.dto.SessionExerciseResponse;
import com.thesis.workout.session.web.dto.WorkoutSessionDetailResponse;
import com.thesis.workout.template.application.TemplateCommand;
import com.thesis.workout.template.application.TemplateDayCommand;
import com.thesis.workout.template.application.TemplateDayExerciseCommand;
import com.thesis.workout.template.application.TemplateDayExerciseService;
import com.thesis.workout.template.application.TemplateDayRoutineService;
import com.thesis.workout.template.application.TemplateDayService;
import com.thesis.workout.template.application.TemplateService;
import com.thesis.workout.template.domain.model.DayFocus;
import com.thesis.workout.template.domain.model.Difficulty;
import com.thesis.workout.template.domain.model.SplitType;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Dev-only seeder, active only under the {@code demo} Spring profile. Creates one demo user with
 * planning data and months of FINISHED workouts so History and Analytics have realistic content.
 *
 * <p>Sessions are created through the real services (so snapshots/constraints are exercised) and
 * then their timestamps are backdated via {@link JdbcTemplate}, since the API always stamps
 * {@code started_at = now()}. Idempotent: skips entirely if the demo user already exists.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String DEMO_EMAIL = "demo@workout.app";
    private static final String COACH_EMAIL = "coach@workout.app";
    private static final String CLIENT2_EMAIL = "client2@workout.app";
    private static final String PROSPECT_EMAIL = "prospect@workout.app";
    private static final String DEMO_PASSWORD = "demo12345";
    private static final int SESSION_COUNT = 16; // ~2 per week over ~8 weeks

    // Seeded official exercises (V2 migration).
    private static final UUID OFFICIAL_SQUAT = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID OFFICIAL_BENCH = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private static final UUID OFFICIAL_ROW = UUID.fromString("00000000-0000-0000-0000-000000000106");

    // Marketplace demo data: many authors publishing many templates with varied stats so the
    // newest / top / trending sorts visibly differ.
    private static final String AUTHOR_EMAIL_PREFIX = "author";
    private static final int MARKETPLACE_TEMPLATE_COUNT = 50;
    private static final String[] AUTHOR_NAMES = {
        "Alex R.", "Sam T.", "Jordan K.", "Maria L.", "Chris P.", "Dana W.",
        "Noah B.", "Ivy S.", "Leo M.", "Tara V.", "Omar H.", "Nina F."
    };
    private static final SplitType[] SPLITS = {
        SplitType.FULL_BODY, SplitType.UPPER_LOWER, SplitType.PPL, SplitType.BRO_SPLIT, SplitType.CUSTOM
    };
    private static final String[] SPLIT_LABELS = {"Full Body", "Upper/Lower", "Push Pull Legs", "Bro Split", "Custom"};
    private static final Difficulty[] DIFFICULTIES = {
        Difficulty.BEGINNER, Difficulty.INTERMEDIATE, Difficulty.ADVANCED
    };

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExerciseService exerciseService;
    private final RoutineService routineService;
    private final GymService gymService;
    private final EquipmentService equipmentService;
    private final TemplateService templateService;
    private final TemplateDayService templateDayService;
    private final TemplateDayExerciseService templateDayExerciseService;
    private final TemplateDayRoutineService templateDayRoutineService;
    private final WorkoutSessionService workoutSessionService;
    private final WorkoutSetService workoutSetService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<SearchRebuildService> searchRebuildService;
    private final CoachProfileRepository coachProfileRepository;
    private final CoachClientRelationshipRepository coachClientRelationshipRepository;

    public DemoDataSeeder(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
            ExerciseService exerciseService, RoutineService routineService, GymService gymService,
            EquipmentService equipmentService, TemplateService templateService,
            TemplateDayService templateDayService, TemplateDayExerciseService templateDayExerciseService,
            TemplateDayRoutineService templateDayRoutineService, WorkoutSessionService workoutSessionService,
            WorkoutSetService workoutSetService, JdbcTemplate jdbcTemplate,
            ObjectProvider<SearchRebuildService> searchRebuildService,
            CoachProfileRepository coachProfileRepository,
            CoachClientRelationshipRepository coachClientRelationshipRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.exerciseService = exerciseService;
        this.routineService = routineService;
        this.gymService = gymService;
        this.equipmentService = equipmentService;
        this.templateService = templateService;
        this.templateDayService = templateDayService;
        this.templateDayExerciseService = templateDayExerciseService;
        this.templateDayRoutineService = templateDayRoutineService;
        this.workoutSessionService = workoutSessionService;
        this.workoutSetService = workoutSetService;
        this.jdbcTemplate = jdbcTemplate;
        this.searchRebuildService = searchRebuildService;
        this.coachProfileRepository = coachProfileRepository;
        this.coachClientRelationshipRepository = coachClientRelationshipRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedDemoAthlete();
        seedDeepWorkoutHistory();
        seedCoach();
        seedAdditionalCoachClients();
        seedMarketplace();
        rebuildSearchIndex();
    }

    /**
     * Seeds a coach account ({@code coach@workout.app} / {@code demo12345}) with an ACTIVE
     * relationship to the demo athlete, so coach mode is demoable immediately: the coach logs in and
     * sees the athlete's full history (incl. the 2020 sessions), analytics and session details.
     * Idempotent: skips if the coach already exists.
     */
    private void seedCoach() {
        if (userRepository.existsByEmailIgnoreCase(COACH_EMAIL)) {
            log.info("Coach already present ({}); skipping.", COACH_EMAIL);
            return;
        }
        UUID clientId = jdbcTemplate.query("SELECT id FROM app_users WHERE lower(email) = lower(?)",
                rs -> rs.next() ? rs.getObject(1, UUID.class) : null, DEMO_EMAIL);
        if (clientId == null) {
            return; // demo athlete absent — nothing to coach
        }
        log.info("Seeding coach {} with an active relationship to {} ...", COACH_EMAIL, DEMO_EMAIL);
        AppUser coach = AppUser.create(
                COACH_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), "Coach Carter", Role.COACH);
        userRepository.save(coach);
        coachProfileRepository.save(CoachProfile.create(coach.getId(), "Strength & conditioning coach (demo)."));
        CoachClientRelationship relationship =
                CoachClientRelationship.invite(coach.getId(), clientId, coach.getId());
        relationship.accept(Instant.now());
        coachClientRelationshipRepository.save(relationship);
        log.info("Coach seeded: login as {} / {} (active client: {}).", COACH_EMAIL, DEMO_PASSWORD, DEMO_EMAIL);
    }

    /**
     * Gives the demo coach more to work with: a second ACTIVE client with its own (smaller) history,
     * and a third user with a PENDING invite so the accept/reject flow is demoable out of the box.
     * Independent of {@link #seedCoach()} and idempotent (keyed on the second client) so it still
     * runs on a DB where the coach already exists. All accounts use password {@code demo12345}.
     */
    private void seedAdditionalCoachClients() {
        UUID coachId = jdbcTemplate.query("SELECT id FROM app_users WHERE lower(email) = lower(?)",
                rs -> rs.next() ? rs.getObject(1, UUID.class) : null, COACH_EMAIL);
        if (coachId == null || userRepository.existsByEmailIgnoreCase(CLIENT2_EMAIL)) {
            return;
        }
        log.info("Seeding additional coach clients ({} active, {} pending) ...", CLIENT2_EMAIL, PROSPECT_EMAIL);

        AppUser client2 = AppUser.create(
                CLIENT2_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), "Jordan Lee", Role.USER);
        userRepository.save(client2);
        insertSimpleSessions(client2.getId(), 24);
        CoachClientRelationship active = CoachClientRelationship.invite(coachId, client2.getId(), coachId);
        active.accept(Instant.now());
        coachClientRelationshipRepository.save(active);

        AppUser prospect = AppUser.create(
                PROSPECT_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), "Sam Rivera", Role.USER);
        userRepository.save(prospect);
        // Pending invite only — log in as the prospect to accept/reject it.
        coachClientRelationshipRepository.save(
                CoachClientRelationship.invite(coachId, prospect.getId(), coachId));

        log.info("Additional clients seeded: {} (active, 24 workouts), {} (pending invite).",
                CLIENT2_EMAIL, PROSPECT_EMAIL);
    }

    /** Bulk-inserts {@code count} simple finished sessions (Bench/Squat/Row) for a user, ~5 days apart. */
    private void insertSimpleSessions(UUID userId, int count) {
        String[][] lifts = {
            {"Barbell Bench Press", "CHEST", "TRICEPS"},
            {"Barbell Back Squat", "QUADS", "GLUTES"},
            {"Barbell Row", "BACK", "LATS"}
        };
        Random rng = new Random(userId.getMostSignificantBits());
        Instant now = Instant.now();
        List<Object[]> sessions = new ArrayList<>();
        List<Object[]> exercises = new ArrayList<>();
        List<Object[]> muscles = new ArrayList<>();
        List<Object[]> sets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID sessionId = UUID.randomUUID();
            Instant startedAt = now.minus((long) (count - i) * 5, ChronoUnit.DAYS);
            Timestamp finishedTs = Timestamp.from(startedAt.plus(55, ChronoUnit.MINUTES));
            sessions.add(new Object[] {sessionId, userId, "Full Body", "Full Body A", "Community Gym",
                    Timestamp.from(startedAt), finishedTs, null, finishedTs, finishedTs});
            int position = 1;
            for (String[] lift : lifts) {
                UUID exId = UUID.randomUUID();
                exercises.add(new Object[] {exId, sessionId, lift[0], "STRENGTH", position++, 3, "5-8",
                        finishedTs, finishedTs});
                for (int m = 1; m < lift.length; m++) {
                    muscles.add(new Object[] {exId, lift[m], m == 1 ? "PRIMARY" : "SECONDARY"});
                }
                double weight = round2_5(40 + i * 0.5 + rng.nextInt(10));
                for (int s = 1; s <= 3; s++) {
                    sets.add(new Object[] {UUID.randomUUID(), exId, s, weight, 5 + rng.nextInt(4), null,
                            "Barbell", finishedTs, finishedTs, finishedTs});
                }
            }
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO workout_sessions (id, user_id, template_name_snapshot, template_day_name_snapshot, "
                        + "gym_name_snapshot, status, started_at, finished_at, notes, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, 'FINISHED', ?, ?, ?, ?, ?)", sessions);
        jdbcTemplate.batchUpdate(
                "INSERT INTO session_exercises (id, session_id, exercise_name_snapshot, exercise_type_snapshot, "
                        + "position, planned_sets_snapshot, planned_reps_snapshot, is_extra_exercise, created_at, "
                        + "updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, false, ?, ?)", exercises);
        jdbcTemplate.batchUpdate(
                "INSERT INTO session_exercise_muscle_groups (session_exercise_id, muscle_group_code_snapshot, "
                        + "role_snapshot) VALUES (?, ?, ?)", muscles);
        jdbcTemplate.batchUpdate(
                "INSERT INTO workout_sets (id, session_exercise_id, set_number, set_type, weight, reps, note, "
                        + "equipment_name_snapshot, completed_at, created_at, updated_at) "
                        + "VALUES (?, ?, ?, 'WORKING', ?, ?, ?, ?, ?, ?, ?)", sets);
    }

    /**
     * Seeds a deep, multi-year workout history (Jan 2020 → today) for the demo athlete via bulk SQL,
     * so search/history have realistic content: date-range filters, the by-month facet, and
     * "find that one specific session" use cases are all meaningful. FK ids are left NULL — the
     * immutable snapshot columns are the source of truth — so no live template/gym is required.
     * Idempotent: skips if any pre-2024 session already exists for the demo athlete.
     */
    private void seedDeepWorkoutHistory() {
        UUID userId = jdbcTemplate.query("SELECT id FROM app_users WHERE lower(email) = lower(?)",
                rs -> rs.next() ? rs.getObject(1, UUID.class) : null, DEMO_EMAIL);
        if (userId == null) {
            return; // demo athlete not present
        }
        Long existing = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM workout_sessions WHERE user_id = ? AND started_at < TIMESTAMP '2024-01-01'",
                Long.class, userId);
        if (existing != null && existing > 0) {
            log.info("Deep workout history already present; skipping.");
            return;
        }
        log.info("Seeding deep workout history (2020 -> now) for {} ...", DEMO_EMAIL);

        record Lift(String name, String type, double base, List<String> muscles, String equipment) {
        }
        record PlannedSession(Instant startedAt, int durationMin, String templateName, String dayName,
                String gym, String note, List<Lift> lifts, boolean bodyweight) {
        }

        Map<String, List<Lift>> dayPlans = Map.of(
                "Push Day", List.of(
                        new Lift("Barbell Bench Press", "STRENGTH", 60, List.of("CHEST", "TRICEPS", "SHOULDERS"), "Olympic Barbell"),
                        new Lift("Overhead Press", "STRENGTH", 35, List.of("SHOULDERS", "TRICEPS"), "Olympic Barbell"),
                        new Lift("Incline Dumbbell Press", "STRENGTH", 22, List.of("CHEST", "SHOULDERS"), "Adjustable Dumbbells"),
                        new Lift("Cable Fly", "STRENGTH", 15, List.of("CHEST"), "Cable Machine"),
                        new Lift("Tricep Pushdown", "STRENGTH", 25, List.of("TRICEPS"), "Cable Machine")),
                "Pull Day", List.of(
                        new Lift("Deadlift", "STRENGTH", 100, List.of("BACK", "HAMSTRINGS", "GLUTES"), "Olympic Barbell"),
                        new Lift("Barbell Row", "STRENGTH", 55, List.of("BACK", "LATS", "BICEPS"), "Olympic Barbell"),
                        new Lift("Lat Pulldown", "STRENGTH", 50, List.of("LATS", "BICEPS"), "Cable Machine"),
                        new Lift("Bicep Curl", "STRENGTH", 15, List.of("BICEPS"), "Adjustable Dumbbells"),
                        new Lift("Face Pull", "STRENGTH", 20, List.of("SHOULDERS"), "Cable Machine")),
                "Leg Day", List.of(
                        new Lift("Barbell Back Squat", "STRENGTH", 80, List.of("QUADS", "GLUTES", "HAMSTRINGS"), "Olympic Barbell"),
                        new Lift("Romanian Deadlift", "STRENGTH", 70, List.of("HAMSTRINGS", "GLUTES"), "Olympic Barbell"),
                        new Lift("Leg Press", "STRENGTH", 150, List.of("QUADS", "GLUTES"), "Leg Press Machine"),
                        new Lift("Lying Leg Curl", "STRENGTH", 40, List.of("HAMSTRINGS"), "Leg Press Machine"),
                        new Lift("Standing Calf Raise", "STRENGTH", 60, List.of("CALVES"), "Leg Press Machine")));

        List<String> dayCycle = List.of("Push Day", "Pull Day", "Leg Day");
        String[] gyms = {"Iron Temple Gym", "Home Garage Gym", "University Rec Center", "PowerHouse Fitness"};
        String[] notePool = {null, null, null, null, null,
                "Felt strong today, hit all my targets.",
                "Tired after work but pushed through.",
                "Deload week — kept the weights lighter.",
                "Great pump, energy was high.",
                "Lower back a little tight, focused on form."};

        Random rng = new Random(2020);
        Instant start = Instant.parse("2020-01-06T17:30:00Z");
        Instant now = Instant.now();
        List<PlannedSession> planned = new ArrayList<>();

        Instant cursor = start;
        int index = 0;
        while (cursor.isBefore(now) && planned.size() < 600) {
            String dayName = dayCycle.get(index % dayCycle.size());
            planned.add(new PlannedSession(cursor, 50 + rng.nextInt(30), "PPL Program", dayName,
                    gyms[index % gyms.length], notePool[rng.nextInt(notePool.length)], dayPlans.get(dayName), false));
            cursor = cursor.plus(3 + rng.nextInt(3), ChronoUnit.DAYS).plus(rng.nextInt(8), ChronoUnit.HOURS);
            index++;
        }

        // Distinctive, memorable sessions so "find that one specific 2020 workout" has a real answer.
        planned.add(new PlannedSession(Instant.parse("2020-03-23T18:00:00Z"), 40, "Home Bodyweight",
                "Lockdown Full Body", "Home (COVID Lockdown)",
                "First home workout — all gyms closed for the COVID-19 lockdown. Bodyweight only, using a backpack for load.",
                List.of(new Lift("Push-up", "STRENGTH", 0, List.of("CHEST", "TRICEPS"), "Bodyweight"),
                        new Lift("Bodyweight Squat", "STRENGTH", 0, List.of("QUADS", "GLUTES"), "Bodyweight"),
                        new Lift("Pull-up", "STRENGTH", 0, List.of("LATS", "BICEPS"), "Pull-up Bar"),
                        new Lift("Plank", "STRENGTH", 0, List.of("CORE"), "Bodyweight")), true));
        planned.add(new PlannedSession(Instant.parse("2020-07-18T08:30:00Z"), 45, "Holiday Training",
                "Beach Session", "Beach Workout — Bali",
                "Outdoor beach training on holiday in Bali — sand sprints and bodyweight circuits at sunrise.",
                List.of(new Lift("Push-up", "STRENGTH", 0, List.of("CHEST", "TRICEPS"), "Bodyweight"),
                        new Lift("Bodyweight Squat", "STRENGTH", 0, List.of("QUADS", "GLUTES"), "Bodyweight"),
                        new Lift("Sand Sprint", "CARDIO", 0, List.of("QUADS", "HAMSTRINGS"), "Bodyweight")), true));
        planned.add(new PlannedSession(Instant.parse("2020-11-07T16:00:00Z"), 65, "PPL Program",
                "Push Day", "Iron Temple Gym",
                "Bench press PR day — hit 100 kg for the first time ever. Massive milestone after a year of training.",
                dayPlans.get("Push Day"), false));
        planned.add(new PlannedSession(Instant.parse("2021-05-15T11:00:00Z"), 70, "Strongman Prep",
                "Strongman Event Prep", "Strongman Yard",
                "Strongman competition prep — atlas stones, farmer's walks and yoke carries in the yard.",
                List.of(new Lift("Atlas Stone Lift", "STRENGTH", 80, List.of("BACK", "GLUTES", "QUADS"), "Atlas Stone"),
                        new Lift("Farmer's Walk", "STRENGTH", 90, List.of("FOREARMS", "TRAPS", "CORE"), "Farmers Handles"),
                        new Lift("Yoke Carry", "STRENGTH", 200, List.of("QUADS", "CORE", "BACK"), "Yoke")), false));

        long spanMillis = now.toEpochMilli() - start.toEpochMilli();
        List<Object[]> sessionRows = new ArrayList<>();
        List<Object[]> exerciseRows = new ArrayList<>();
        List<Object[]> muscleRows = new ArrayList<>();
        List<Object[]> setRows = new ArrayList<>();

        for (PlannedSession session : planned) {
            UUID sessionId = UUID.randomUUID();
            Instant startedAt = session.startedAt();
            Instant finishedAt = startedAt.plus(session.durationMin(), ChronoUnit.MINUTES);
            Timestamp startTs = Timestamp.from(startedAt);
            Timestamp finishTs = Timestamp.from(finishedAt);
            double progress = Math.max(0, Math.min(1.0,
                    (startedAt.toEpochMilli() - start.toEpochMilli()) / (double) spanMillis));
            sessionRows.add(new Object[] {sessionId, userId, session.templateName(), session.dayName(),
                    session.gym(), startTs, finishTs, session.note(), finishTs, finishTs});

            int position = 1;
            for (Lift lift : session.lifts()) {
                UUID exId = UUID.randomUUID();
                int setCount = 3 + rng.nextInt(2); // 3..4
                String plannedReps = session.bodyweight() ? "10-20" : "5-10";
                exerciseRows.add(new Object[] {exId, sessionId, lift.name(), lift.type(), position++,
                        setCount, plannedReps, finishTs, finishTs});
                for (int m = 0; m < lift.muscles().size(); m++) {
                    muscleRows.add(new Object[] {exId, lift.muscles().get(m), m == 0 ? "PRIMARY" : "SECONDARY"});
                }
                double topWeight = round2_5(lift.base() * (1.0 + 0.5 * progress));
                for (int s = 1; s <= setCount; s++) {
                    Double weight = session.bodyweight() || lift.base() == 0 ? null : topWeight;
                    int reps = session.bodyweight() ? 10 + rng.nextInt(11) : 5 + rng.nextInt(6);
                    String setNote = rng.nextInt(12) == 0 ? "last rep was a grind" : null;
                    Timestamp completedTs = Timestamp.from(startedAt.plus((long) position * 6L + s, ChronoUnit.MINUTES));
                    setRows.add(new Object[] {UUID.randomUUID(), exId, s, weight, reps, setNote,
                            lift.equipment(), completedTs, completedTs, completedTs});
                }
            }
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO workout_sessions (id, user_id, template_name_snapshot, template_day_name_snapshot, "
                        + "gym_name_snapshot, status, started_at, finished_at, notes, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, 'FINISHED', ?, ?, ?, ?, ?)", sessionRows);
        jdbcTemplate.batchUpdate(
                "INSERT INTO session_exercises (id, session_id, exercise_name_snapshot, exercise_type_snapshot, "
                        + "position, planned_sets_snapshot, planned_reps_snapshot, is_extra_exercise, created_at, "
                        + "updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, false, ?, ?)", exerciseRows);
        jdbcTemplate.batchUpdate(
                "INSERT INTO session_exercise_muscle_groups (session_exercise_id, muscle_group_code_snapshot, "
                        + "role_snapshot) VALUES (?, ?, ?)", muscleRows);
        jdbcTemplate.batchUpdate(
                "INSERT INTO workout_sets (id, session_exercise_id, set_number, set_type, weight, reps, note, "
                        + "equipment_name_snapshot, completed_at, created_at, updated_at) "
                        + "VALUES (?, ?, ?, 'WORKING', ?, ?, ?, ?, ?, ?, ?)", setRows);

        log.info("Deep history seeded: {} sessions, {} exercises, {} sets (2020 -> now).",
                sessionRows.size(), exerciseRows.size(), setRows.size());
    }

    private static double round2_5(double weight) {
        return Math.round(weight / 2.5) * 2.5;
    }

    /**
     * Populates the OpenSearch index from the seeded PostgreSQL data. The marketplace templates are
     * inserted with raw SQL (bypassing the event-publishing services), so a full rebuild is the only
     * way they reach the index. No-op when search is disabled (the bean is absent).
     */
    private void rebuildSearchIndex() {
        SearchRebuildService rebuild = searchRebuildService.getIfAvailable();
        if (rebuild == null) {
            return;
        }
        try {
            SearchRebuildService.RebuildResult result = rebuild.rebuildAll();
            log.info("Demo search index rebuilt: {} templates, {} sessions",
                    result.templates(), result.sessions());
        } catch (RuntimeException ex) {
            log.warn("Demo search index rebuild failed (search may be unavailable): {}", ex.getMessage());
        }
    }

    private void seedDemoAthlete() {
        if (userRepository.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            log.info("Demo athlete already present ({}); skipping.", DEMO_EMAIL);
            return;
        }
        log.info("Seeding demo athlete {} ...", DEMO_EMAIL);

        AppUser user = AppUser.create(
                DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), "Demo Athlete", Role.USER);
        userRepository.save(user);
        UUID userId = user.getId();

        // Planning data.
        UUID inclinePress = exerciseService.createCustom(userId, new CustomExerciseCommand(
                "Incline Dumbbell Press", "Upper-chest focused press.", ExerciseType.STRENGTH,
                List.of(new MuscleAssignment("CHEST", MuscleRole.PRIMARY),
                        new MuscleAssignment("SHOULDERS", MuscleRole.SECONDARY),
                        new MuscleAssignment("TRICEPS", MuscleRole.SECONDARY)))).id();
        UUID warmup = routineService.create(userId, new RoutineCommand(
                "Full-body warm-up", RoutineType.START, "5 min bike, leg swings, band pull-aparts.")).id();
        UUID gymId = gymService.create(userId, new GymCommand("Home Garage Gym", "Garage")).id();
        equipmentService.create(userId, gymId, new EquipmentCommand("Olympic Barbell", EquipmentType.BARBELL, null));
        equipmentService.create(userId, gymId, new EquipmentCommand("Adjustable Dumbbells", EquipmentType.DUMBBELL, null));

        UUID templateId = templateService.create(userId, new TemplateCommand(
                "Demo Full Body", "A simple 1-day full-body program for the demo.",
                SplitType.FULL_BODY, 3, Difficulty.INTERMEDIATE, 60)).id();
        UUID dayId = templateDayService.create(userId, templateId, new TemplateDayCommand(
                1, "Full Body A", DayFocus.FULL_BODY, 60, "Compound focus.")).id();
        templateDayRoutineService.attach(userId, dayId, warmup);

        // Day exercises (the workout structure repeated across sessions).
        List<Lift> lifts = List.of(
                new Lift(OFFICIAL_SQUAT, new BigDecimal("80"), new BigDecimal("2.5"), 5),
                new Lift(OFFICIAL_BENCH, new BigDecimal("60"), new BigDecimal("1.5"), 5),
                new Lift(OFFICIAL_ROW, new BigDecimal("55"), new BigDecimal("1.5"), 8),
                new Lift(inclinePress, new BigDecimal("22.5"), new BigDecimal("1.0"), 10));
        for (Lift lift : lifts) {
            templateDayExerciseService.add(userId, dayId, new TemplateDayExerciseCommand(
                    lift.exerciseId(), 3, String.valueOf(lift.reps()), lift.baseWeight(), 120, null));
        }

        // Backdated finished sessions with progressive overload.
        Instant now = Instant.now();
        for (int i = 0; i < SESSION_COUNT; i++) {
            int weekIndex = i / 2;
            // ~3.5 days (84h) apart, oldest first; spans ~8 weeks back, none in the future.
            Instant startedAt = now.minus((long) (SESSION_COUNT - i) * 84, ChronoUnit.HOURS);
            Instant finishedAt = startedAt.plus(Duration.ofMinutes(58));

            WorkoutSessionDetailResponse session = workoutSessionService.start(userId, dayId, gymId);
            List<SessionExerciseResponse> sessionExercises = session.exercises();
            for (int e = 0; e < sessionExercises.size(); e++) {
                Lift lift = lifts.get(e);
                BigDecimal weight = lift.baseWeight().add(lift.weeklyIncrement().multiply(BigDecimal.valueOf(weekIndex)));
                for (int set = 0; set < 3; set++) {
                    workoutSetService.addSet(userId, sessionExercises.get(e).id(),
                            new SetCommand(SetType.WORKING, weight, lift.reps(), null, null,
                                    new BigDecimal("8.0"), null, null));
                }
            }
            workoutSessionService.finish(userId, session.id());
            backdate(session.id(), startedAt, finishedAt);
        }

        log.info("Demo data seeded: login as {} / {} ({} workouts).", DEMO_EMAIL, DEMO_PASSWORD, SESSION_COUNT);
    }

    private void backdate(UUID sessionId, Instant startedAt, Instant finishedAt) {
        jdbcTemplate.update(
                "UPDATE workout_sessions SET started_at = ?, finished_at = ?, created_at = ?, updated_at = ? "
                        + "WHERE id = ?",
                Timestamp.from(startedAt), Timestamp.from(finishedAt), Timestamp.from(startedAt),
                Timestamp.from(finishedAt), sessionId);
        jdbcTemplate.update(
                "UPDATE workout_sets SET completed_at = ?, created_at = ?, updated_at = ? "
                        + "WHERE session_exercise_id IN (SELECT id FROM session_exercises WHERE session_id = ?)",
                Timestamp.from(startedAt), Timestamp.from(startedAt), Timestamp.from(startedAt), sessionId);
    }

    /**
     * Seeds a populated marketplace: several authors each publish templates, with varied
     * upvotes/downvotes/saves/uses and spread-out publish dates so the newest / top / trending
     * sorts visibly differ. Templates are built through the real services (publishable, official
     * exercises), then publish state, dates and stats are set directly for controlled test data.
     */
    private void seedMarketplace() {
        if (userRepository.existsByEmailIgnoreCase(AUTHOR_EMAIL_PREFIX + "1@workout.app")) {
            log.info("Marketplace demo data already present; skipping.");
            return;
        }
        log.info("Seeding {} marketplace templates across {} authors ...",
                MARKETPLACE_TEMPLATE_COUNT, AUTHOR_NAMES.length);

        List<UUID> authors = new ArrayList<>();
        for (int a = 0; a < AUTHOR_NAMES.length; a++) {
            AppUser author = AppUser.create(AUTHOR_EMAIL_PREFIX + (a + 1) + "@workout.app",
                    passwordEncoder.encode(DEMO_PASSWORD), AUTHOR_NAMES[a], Role.USER);
            userRepository.save(author);
            authors.add(author.getId());
        }

        UUID[] officialLifts = {OFFICIAL_SQUAT, OFFICIAL_BENCH, OFFICIAL_ROW};
        Random rnd = new Random(42); // fixed seed → reproducible demo data
        Instant now = Instant.now();

        for (int t = 0; t < MARKETPLACE_TEMPLATE_COUNT; t++) {
            UUID authorId = authors.get(t % authors.size());
            SplitType split = SPLITS[t % SPLITS.length];
            Difficulty difficulty = DIFFICULTIES[t % DIFFICULTIES.length];
            int daysPerWeek = 2 + (t % 5); // 2..6
            String name = SPLIT_LABELS[t % SPLIT_LABELS.length] + " Program " + (t + 1);

            UUID templateId = templateService.create(authorId, new TemplateCommand(
                    name, "Community-shared " + split + " program.", split, daysPerWeek, difficulty,
                    45 + (t % 5) * 10)).id();
            UUID dayId = templateDayService.create(authorId, templateId, new TemplateDayCommand(
                    1, "Day 1", DayFocus.FULL_BODY, 60, null)).id();
            templateDayExerciseService.add(authorId, dayId, new TemplateDayExerciseCommand(
                    officialLifts[t % officialLifts.length], 3, "5", new BigDecimal("60"), 120, null));
            templateDayExerciseService.add(authorId, dayId, new TemplateDayExerciseCommand(
                    officialLifts[(t + 1) % officialLifts.length], 3, "8", new BigDecimal("40"), 120, null));

            Instant publishedAt = now.minus(rnd.nextInt(60) * 24L + rnd.nextInt(24), ChronoUnit.HOURS);
            publishWithStats(templateId, publishedAt,
                    rnd.nextInt(250), rnd.nextInt(40), rnd.nextInt(120), rnd.nextInt(60));
        }
        log.info("Marketplace demo data seeded.");
    }

    /** Publishes a template and sets its publish date + varied stats directly (demo sorting data). */
    private void publishWithStats(UUID templateId, Instant publishedAt, int up, int down, int saves, int uses) {
        jdbcTemplate.update(
                "UPDATE workout_templates SET visibility = 'PUBLIC', published_at = ?, updated_at = now() "
                        + "WHERE id = ?",
                Timestamp.from(publishedAt), templateId);
        jdbcTemplate.update(
                "UPDATE template_stats SET upvotes_count = ?, downvotes_count = ?, saves_count = ?, "
                        + "uses_count = ?, rating_score = ? - ?, "
                        + "trending_score = round((? - ?) "
                        + "/ power(extract(epoch FROM (now() - ?)) / 3600.0 + 2, 1.5), 4), updated_at = now() "
                        + "WHERE template_id = ?",
                up, down, saves, uses, up, down, up, down, Timestamp.from(publishedAt), templateId);
    }

    private record Lift(UUID exerciseId, BigDecimal baseWeight, BigDecimal weeklyIncrement, int reps) {
    }
}
