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
import com.thesis.workout.routine.application.RoutineCommand;
import com.thesis.workout.routine.application.RoutineService;
import com.thesis.workout.routine.domain.model.RoutineType;
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
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final String DEMO_PASSWORD = "demo12345";
    private static final int SESSION_COUNT = 16; // ~2 per week over ~8 weeks

    // Seeded official exercises (V2 migration).
    private static final UUID OFFICIAL_SQUAT = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID OFFICIAL_BENCH = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private static final UUID OFFICIAL_ROW = UUID.fromString("00000000-0000-0000-0000-000000000106");

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

    public DemoDataSeeder(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
            ExerciseService exerciseService, RoutineService routineService, GymService gymService,
            EquipmentService equipmentService, TemplateService templateService,
            TemplateDayService templateDayService, TemplateDayExerciseService templateDayExerciseService,
            TemplateDayRoutineService templateDayRoutineService, WorkoutSessionService workoutSessionService,
            WorkoutSetService workoutSetService, JdbcTemplate jdbcTemplate) {
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
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            log.info("Demo data already present ({}); skipping seed.", DEMO_EMAIL);
            return;
        }
        log.info("Seeding demo data for {} ...", DEMO_EMAIL);

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

    private record Lift(UUID exerciseId, BigDecimal baseWeight, BigDecimal weeklyIncrement, int reps) {
    }
}
