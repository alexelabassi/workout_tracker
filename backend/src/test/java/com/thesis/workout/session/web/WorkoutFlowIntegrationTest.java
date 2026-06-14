package com.thesis.workout.session.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.workout.AbstractPostgresIntegrationTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class WorkoutFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret!";
    private static final String OFFICIAL_SQUAT_ID = "00000000-0000-0000-0000-000000000101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record Fixtures(String token, UUID templateId, UUID dayId, UUID gymId, UUID equipmentId,
            UUID exerciseId, String exerciseName, String routineContent, String gymName, String equipmentName,
            String templateName, String dayName) {
    }

    @Test
    void startCopiesTemplateDayIntoSessionSnapshots() throws Exception {
        Fixtures fx = setup();

        MvcResult started = startWorkout(fx.token(), fx.dayId(), fx.gymId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.templateName").value(fx.templateName()))
                .andExpect(jsonPath("$.templateDayName").value(fx.dayName()))
                .andExpect(jsonPath("$.gymName").value(fx.gymName()))
                .andExpect(jsonPath("$.exercises.length()").value(1))
                .andExpect(jsonPath("$.exercises[0].exerciseName").value(fx.exerciseName()))
                .andExpect(jsonPath("$.exercises[0].exerciseType").value("STRENGTH"))
                .andExpect(jsonPath("$.exercises[0].plannedSets").value(3))
                .andExpect(jsonPath("$.exercises[0].plannedReps").value("10"))
                .andExpect(jsonPath("$.exercises[0].muscleGroups.length()").value(2))
                .andExpect(jsonPath("$.exercises[0].extraExercise").value(false))
                .andExpect(jsonPath("$.routines.length()").value(1))
                .andExpect(jsonPath("$.routines[0].routineType").value("START"))
                .andExpect(jsonPath("$.routines[0].routineContent").value(fx.routineContent()))
                .andReturn();
        assertThat(body(started).get("id").asText()).isNotBlank();
    }

    @Test
    void sessionSnapshotsRemainStableAfterSourceEdits() throws Exception {
        Fixtures fx = setup();
        UUID sessionId = idOf(startWorkout(fx.token(), fx.dayId(), fx.gymId()).andReturn());
        UUID sessionExerciseId = firstSessionExerciseId(fx.token(), sessionId);

        // Log a set referencing the equipment so we can verify its snapshot survives an edit.
        Map<String, Object> setBody = new HashMap<>();
        setBody.put("weight", 100);
        setBody.put("reps", 5);
        setBody.put("equipmentId", fx.equipmentId().toString());
        mockMvc.perform(post("/api/session-exercises/" + sessionExerciseId + "/sets")
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setBody)))
                .andExpect(status().isCreated());

        // Mutate every source entity.
        mockMvc.perform(put("/api/templates/" + fx.templateId())
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "RENAMED TEMPLATE"))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/template-days/" + fx.dayId())
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("dayNumber", 1, "name", "RENAMED DAY"))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/exercises/custom/" + fx.exerciseId())
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "RENAMED EXERCISE", "exerciseType", "CARDIO",
                                "muscleGroups", List.of(Map.of("code", "BACK", "role", "PRIMARY"))))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/gyms/" + fx.gymId())
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "RENAMED GYM"))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/equipment/" + fx.equipmentId())
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "RENAMED EQUIPMENT"))))
                .andExpect(status().isOk());

        // The session must still reflect the original snapshots.
        MvcResult detail = mockMvc.perform(get("/api/workouts/" + sessionId)
                        .header("Authorization", "Bearer " + fx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateName").value(fx.templateName()))
                .andExpect(jsonPath("$.templateDayName").value(fx.dayName()))
                .andExpect(jsonPath("$.gymName").value(fx.gymName()))
                .andExpect(jsonPath("$.exercises[0].exerciseName").value(fx.exerciseName()))
                .andExpect(jsonPath("$.exercises[0].exerciseType").value("STRENGTH"))
                .andExpect(jsonPath("$.exercises[0].muscleGroups.length()").value(2))
                .andReturn();
        JsonNode set = body(detail).get("exercises").get(0).get("sets").get(0);
        assertThat(set.get("equipmentName").asText()).isEqualTo(fx.equipmentName());
        assertThat(set.get("completedAt").isNull()).isFalse();
    }

    @Test
    void cannotStartFromAnotherUsersTemplateDay() throws Exception {
        Fixtures owner = setup();
        Fixtures intruder = setup();

        mockMvc.perform(post("/api/workouts/start")
                        .header("Authorization", "Bearer " + intruder.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "templateDayId", owner.dayId().toString(), "gymId", intruder.gymId().toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TEMPLATE_DAY_NOT_FOUND"));
    }

    @Test
    void cannotStartWithAnotherUsersGym() throws Exception {
        Fixtures owner = setup();
        Fixtures other = setup();

        mockMvc.perform(post("/api/workouts/start")
                        .header("Authorization", "Bearer " + other.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "templateDayId", other.dayId().toString(), "gymId", owner.gymId().toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("GYM_NOT_FOUND"));
    }

    @Test
    void onlyOneActiveWorkoutAllowed() throws Exception {
        Fixtures fx = setup();
        startWorkout(fx.token(), fx.dayId(), fx.gymId()).andExpect(status().isCreated());

        startWorkout(fx.token(), fx.dayId(), fx.gymId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ACTIVE_WORKOUT_EXISTS"));
    }

    @Test
    void databaseRejectsASecondActiveSession() throws Exception {
        Fixtures fx = setup();
        UUID sessionId = idOf(startWorkout(fx.token(), fx.dayId(), fx.gymId()).andReturn());
        UUID userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM workout_sessions WHERE id = ?", UUID.class, sessionId);

        // Bypass the service guard and hit the partial unique index directly: a second
        // IN_PROGRESS row for the same user must be rejected by the database.
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO workout_sessions (id, user_id, status, started_at, created_at, updated_at) "
                        + "VALUES (?, ?, 'IN_PROGRESS', now(), now(), now())",
                UUID.randomUUID(), userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void activeEndpointReflectsCurrentSession() throws Exception {
        Fixtures fx = setup();

        mockMvc.perform(get("/api/workouts/active").header("Authorization", "Bearer " + fx.token()))
                .andExpect(status().isNoContent());

        UUID sessionId = idOf(startWorkout(fx.token(), fx.dayId(), fx.gymId()).andReturn());
        mockMvc.perform(get("/api/workouts/active").header("Authorization", "Bearer " + fx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void cannotStartFromEmptyTemplateDay() throws Exception {
        String token = registerAndToken();
        UUID templateId = createTemplate(token, uniqueName("Empty"));
        UUID dayId = createDay(token, templateId, 1, "Empty Day");
        UUID gymId = createGym(token, uniqueName("Gym"));

        mockMvc.perform(post("/api/workouts/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "templateDayId", dayId.toString(), "gymId", gymId.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TEMPLATE_DAY_EMPTY"));
    }

    @Test
    void addSetCapturesEquipmentSnapshot() throws Exception {
        Fixtures fx = setup();
        UUID sessionId = idOf(startWorkout(fx.token(), fx.dayId(), fx.gymId()).andReturn());
        UUID sessionExerciseId = firstSessionExerciseId(fx.token(), sessionId);

        Map<String, Object> setBody = new HashMap<>();
        setBody.put("setType", "WORKING");
        setBody.put("weight", 80);
        setBody.put("reps", 8);
        setBody.put("equipmentId", fx.equipmentId().toString());
        mockMvc.perform(post("/api/session-exercises/" + sessionExerciseId + "/sets")
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.setNumber").value(1))
                .andExpect(jsonPath("$.equipmentName").value(fx.equipmentName()))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    void cannotAddSetToAnotherUsersSessionExercise() throws Exception {
        Fixtures owner = setup();
        UUID sessionId = idOf(startWorkout(owner.token(), owner.dayId(), owner.gymId()).andReturn());
        UUID sessionExerciseId = firstSessionExerciseId(owner.token(), sessionId);

        Fixtures intruder = setup();
        mockMvc.perform(post("/api/session-exercises/" + sessionExerciseId + "/sets")
                        .header("Authorization", "Bearer " + intruder.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("weight", 50, "reps", 5))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SESSION_EXERCISE_NOT_FOUND"));
    }

    @Test
    void equipmentFromAnotherGymIsRejected() throws Exception {
        Fixtures fx = setup();
        UUID otherGym = createGym(fx.token(), uniqueName("Other Gym"));
        UUID otherGymEquipment = createEquipment(fx.token(), otherGym, uniqueName("Foreign Rack"), "MACHINE");
        UUID sessionId = idOf(startWorkout(fx.token(), fx.dayId(), fx.gymId()).andReturn());
        UUID sessionExerciseId = firstSessionExerciseId(fx.token(), sessionId);

        // Owned equipment, but in a different gym than the session.
        mockMvc.perform(post("/api/session-exercises/" + sessionExerciseId + "/sets")
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("weight", 50, "reps", 5, "equipmentId", otherGymEquipment.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("EQUIPMENT_NOT_IN_SESSION_GYM"));

        // Equipment owned by someone else is simply not found.
        Fixtures other = setup();
        mockMvc.perform(post("/api/session-exercises/" + sessionExerciseId + "/sets")
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("weight", 50, "reps", 5, "equipmentId", other.equipmentId().toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EQUIPMENT_NOT_FOUND"));
    }

    @Test
    void addExtraExerciseSnapshotsNameTypeAndMuscles() throws Exception {
        Fixtures fx = setup();
        UUID sessionId = idOf(startWorkout(fx.token(), fx.dayId(), fx.gymId()).andReturn());

        mockMvc.perform(post("/api/workouts/" + sessionId + "/extra-exercises")
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("exerciseId", OFFICIAL_SQUAT_ID))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exerciseName").value("Barbell Back Squat"))
                .andExpect(jsonPath("$.exerciseType").value("STRENGTH"))
                .andExpect(jsonPath("$.extraExercise").value(true))
                .andExpect(jsonPath("$.muscleGroups.length()").value(4));
    }

    @Test
    void finishMarksSessionFinished() throws Exception {
        Fixtures fx = setup();
        UUID sessionId = idOf(startWorkout(fx.token(), fx.dayId(), fx.gymId()).andReturn());

        mockMvc.perform(post("/api/workouts/" + sessionId + "/finish").header("Authorization", "Bearer " + fx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.finishedAt").isNotEmpty());
    }

    @Test
    void cancelMarksSessionCancelledWithFinishedAt() throws Exception {
        Fixtures fx = setup();
        UUID sessionId = idOf(startWorkout(fx.token(), fx.dayId(), fx.gymId()).andReturn());

        mockMvc.perform(post("/api/workouts/" + sessionId + "/cancel").header("Authorization", "Bearer " + fx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.finishedAt").isNotEmpty());
    }

    @Test
    void setsCannotBeMutatedAfterFinish() throws Exception {
        Fixtures fx = setup();
        UUID sessionId = idOf(startWorkout(fx.token(), fx.dayId(), fx.gymId()).andReturn());
        UUID sessionExerciseId = firstSessionExerciseId(fx.token(), sessionId);
        UUID setId = idOf(mockMvc.perform(post("/api/session-exercises/" + sessionExerciseId + "/sets")
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("weight", 60, "reps", 6))))
                .andExpect(status().isCreated())
                .andReturn());

        mockMvc.perform(post("/api/workouts/" + sessionId + "/finish").header("Authorization", "Bearer " + fx.token()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/session-exercises/" + sessionExerciseId + "/sets")
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("weight", 70, "reps", 5))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SESSION_NOT_ACTIVE"));
        mockMvc.perform(put("/api/sets/" + setId)
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("weight", 65, "reps", 6))))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/sets/" + setId).header("Authorization", "Bearer " + fx.token()))
                .andExpect(status().isConflict());
    }

    @Test
    void workoutEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/workouts/active"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void invalidPayloadsFailValidation() throws Exception {
        Fixtures fx = setup();
        UUID sessionId = idOf(startWorkout(fx.token(), fx.dayId(), fx.gymId()).andReturn());
        UUID sessionExerciseId = firstSessionExerciseId(fx.token(), sessionId);

        // Missing required start fields.
        mockMvc.perform(post("/api/workouts/start")
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("gymId", fx.gymId().toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        // RPE out of range.
        mockMvc.perform(post("/api/session-exercises/" + sessionExerciseId + "/sets")
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("weight", 50, "reps", 5, "rpe", 11))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void loggedSetCanCarryANote() throws Exception {
        Fixtures fx = setup();
        UUID sessionId = idOf(startWorkout(fx.token(), fx.dayId(), fx.gymId()).andReturn());
        UUID sessionExerciseId = firstSessionExerciseId(fx.token(), sessionId);

        mockMvc.perform(post("/api/session-exercises/" + sessionExerciseId + "/sets")
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "weight", 60, "reps", 8, "note", "felt easy, add weight"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value("felt easy, add weight"));

        MvcResult detail = mockMvc.perform(get("/api/workouts/" + sessionId)
                        .header("Authorization", "Bearer " + fx.token()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(body(detail).get("exercises").get(0).get("sets").get(0).get("note").asText())
                .isEqualTo("felt easy, add weight");
    }

    @Test
    void workoutNotesCanBeSetAndEditedEvenAfterFinish() throws Exception {
        Fixtures fx = setup();
        UUID sessionId = idOf(startWorkout(fx.token(), fx.dayId(), fx.gymId()).andReturn());

        mockMvc.perform(put("/api/workouts/" + sessionId + "/notes")
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notes", "great session"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("great session"));

        mockMvc.perform(post("/api/workouts/" + sessionId + "/finish").header("Authorization", "Bearer " + fx.token()))
                .andExpect(status().isOk());

        // Notes remain editable after the workout is finished (they are commentary, not workout data).
        mockMvc.perform(put("/api/workouts/" + sessionId + "/notes")
                        .header("Authorization", "Bearer " + fx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notes", "edited afterwards"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("edited afterwards"));

        mockMvc.perform(get("/api/workouts/" + sessionId).header("Authorization", "Bearer " + fx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("edited afterwards"));
    }

    @Test
    void cannotEditAnotherUsersWorkoutNotes() throws Exception {
        Fixtures owner = setup();
        UUID sessionId = idOf(startWorkout(owner.token(), owner.dayId(), owner.gymId()).andReturn());

        Fixtures intruder = setup();
        mockMvc.perform(put("/api/workouts/" + sessionId + "/notes")
                        .header("Authorization", "Bearer " + intruder.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notes", "hijack"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WORKOUT_NOT_FOUND"));
    }

    // --- fixtures / helpers ---

    private Fixtures setup() throws Exception {
        String token = registerAndToken();
        String exerciseName = uniqueName("Bench Press");
        UUID exerciseId = createCustomExercise(token, exerciseName);
        String routineContent = "Bike 5 min, mobility.";
        UUID routineId = createRoutine(token, uniqueName("Warmup"), routineContent);
        String templateName = uniqueName("Program");
        UUID templateId = createTemplate(token, templateName);
        String dayName = "Day One";
        UUID dayId = createDay(token, templateId, 1, dayName);
        addTemplateExercise(token, dayId, exerciseId);
        attachTemplateRoutine(token, dayId, routineId);
        String gymName = uniqueName("Home Gym");
        UUID gymId = createGym(token, gymName);
        String equipmentName = uniqueName("Barbell");
        UUID equipmentId = createEquipment(token, gymId, equipmentName, "BARBELL");
        return new Fixtures(token, templateId, dayId, gymId, equipmentId, exerciseId, exerciseName,
                routineContent, gymName, equipmentName, templateName, dayName);
    }

    private org.springframework.test.web.servlet.ResultActions startWorkout(String token, UUID dayId, UUID gymId)
            throws Exception {
        return mockMvc.perform(post("/api/workouts/start")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "templateDayId", dayId.toString(), "gymId", gymId.toString()))));
    }

    private UUID firstSessionExerciseId(String token, UUID sessionId) throws Exception {
        MvcResult detail = mockMvc.perform(get("/api/workouts/" + sessionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(body(detail).get("exercises").get(0).get("id").asText());
    }

    private UUID createCustomExercise(String token, String name) throws Exception {
        return idOf(mockMvc.perform(post("/api/exercises/custom")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "exerciseType", "STRENGTH",
                                "muscleGroups", List.of(
                                        Map.of("code", "CHEST", "role", "PRIMARY"),
                                        Map.of("code", "TRICEPS", "role", "SECONDARY"))))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private UUID createRoutine(String token, String name, String content) throws Exception {
        return idOf(mockMvc.perform(post("/api/routines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "routineType", "START", "content", content))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private UUID createTemplate(String token, String name) throws Exception {
        return idOf(mockMvc.perform(post("/api/templates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private UUID createDay(String token, UUID templateId, int dayNumber, String name) throws Exception {
        return idOf(mockMvc.perform(post("/api/templates/" + templateId + "/days")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("dayNumber", dayNumber, "name", name))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void addTemplateExercise(String token, UUID dayId, UUID exerciseId) throws Exception {
        mockMvc.perform(post("/api/template-days/" + dayId + "/exercises")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "exerciseId", exerciseId.toString(), "plannedSets", 3, "plannedReps", "10"))))
                .andExpect(status().isCreated());
    }

    private void attachTemplateRoutine(String token, UUID dayId, UUID routineId) throws Exception {
        mockMvc.perform(post("/api/template-days/" + dayId + "/routines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("routineId", routineId.toString()))))
                .andExpect(status().isCreated());
    }

    private UUID createGym(String token, String name) throws Exception {
        return idOf(mockMvc.perform(post("/api/gyms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private UUID createEquipment(String token, UUID gymId, String name, String type) throws Exception {
        return idOf(mockMvc.perform(post("/api/gyms/" + gymId + "/equipment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "equipmentType", type))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String registerAndToken() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", PASSWORD, "displayName", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("accessToken").asText();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UUID idOf(MvcResult result) throws Exception {
        return UUID.fromString(body(result).get("id").asText());
    }

    private static String uniqueName(String base) {
        return base + " " + UUID.randomUUID();
    }
}
