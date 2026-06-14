package com.thesis.workout.marketplace.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.workout.AbstractPostgresIntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class MarketplaceFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret!";
    private static final String OFFICIAL_SQUAT_ID = "00000000-0000-0000-0000-000000000101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publishRequiresOwnershipAndNonEmptyTemplate() throws Exception {
        String owner = registerAndToken();
        UUID emptyTemplate = createTemplate(owner, "Empty");
        mockMvc.perform(post("/api/templates/" + emptyTemplate + "/publish").header("Authorization", "Bearer " + owner))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TEMPLATE_NOT_PUBLISHABLE"));

        UUID template = publishableTemplate(owner);
        mockMvc.perform(post("/api/templates/" + template + "/publish").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));

        String intruder = registerAndToken();
        mockMvc.perform(post("/api/templates/" + template + "/publish").header("Authorization", "Bearer " + intruder))
                .andExpect(status().isNotFound());
    }

    @Test
    void browseShowsOnlyPublicTemplates() throws Exception {
        String owner = registerAndToken();
        UUID privateTemplate = publishableTemplate(owner);
        UUID publicTemplate = publishableTemplate(owner);
        publish(owner, publicTemplate);

        String viewer = registerAndToken();
        MvcResult result = mockMvc.perform(get("/api/marketplace/templates").header("Authorization", "Bearer " + viewer))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = body(result).get("items");
        assertThat(containsId(items, publicTemplate)).isTrue();
        assertThat(containsId(items, privateTemplate)).isFalse();
    }

    @Test
    void privateTemplateDetailIsNotFoundInMarketplace() throws Exception {
        String owner = registerAndToken();
        UUID privateTemplate = publishableTemplate(owner);
        String viewer = registerAndToken();

        mockMvc.perform(get("/api/marketplace/templates/" + privateTemplate).header("Authorization", "Bearer " + viewer))
                .andExpect(status().isNotFound());
    }

    @Test
    void voteTogglesSwitchesAndRejectsSelfVote() throws Exception {
        String owner = registerAndToken();
        UUID template = publishableTemplate(owner);
        publish(owner, template);

        // Owner cannot vote on own template.
        mockMvc.perform(vote(owner, template, "UP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CANNOT_VOTE_OWN_TEMPLATE"));

        String voter = registerAndToken();
        mockMvc.perform(vote(voter, template, "UP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.upvotes").value(1))
                .andExpect(jsonPath("$.myVote").value("UP"));
        // Switch to DOWN.
        mockMvc.perform(vote(voter, template, "DOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.upvotes").value(0))
                .andExpect(jsonPath("$.stats.downvotes").value(1))
                .andExpect(jsonPath("$.myVote").value("DOWN"));
        // Same vote again toggles off.
        mockMvc.perform(vote(voter, template, "DOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.downvotes").value(0))
                .andExpect(jsonPath("$.myVote").doesNotExist());
    }

    @Test
    void saveIsIdempotentAndCountsStayNonNegative() throws Exception {
        String owner = registerAndToken();
        UUID template = publishableTemplate(owner);
        publish(owner, template);
        String user = registerAndToken();

        mockMvc.perform(post("/api/marketplace/templates/" + template + "/save").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.saves").value(1))
                .andExpect(jsonPath("$.saved").value(true));
        // Saving again must not double-count.
        mockMvc.perform(post("/api/marketplace/templates/" + template + "/save").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.saves").value(1));
        // Unsave twice must not go negative.
        mockMvc.perform(delete("/api/marketplace/templates/" + template + "/save").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.saves").value(0))
                .andExpect(jsonPath("$.saved").value(false));
        mockMvc.perform(delete("/api/marketplace/templates/" + template + "/save").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.saves").value(0));
    }

    @Test
    void savedOnlyFilterReturnsBookmarks() throws Exception {
        String owner = registerAndToken();
        UUID template = publishableTemplate(owner);
        publish(owner, template);
        String user = registerAndToken();
        mockMvc.perform(post("/api/marketplace/templates/" + template + "/save").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/marketplace/templates?savedOnly=true")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(containsId(body(result).get("items"), template)).isTrue();
    }

    @Test
    void useDeepCopiesWithOptionAExerciseHandling() throws Exception {
        // Publisher template mixes an OFFICIAL exercise and a private CUSTOM exercise + a routine.
        String publisher = registerAndToken();
        UUID customExercise = createCustomExercise(publisher, uniqueName("Publisher Curl"));
        UUID routine = createRoutine(publisher, uniqueName("Warmup"), "Bike 5 min.");
        UUID template = createTemplate(publisher, uniqueName("PPL"));
        UUID day = createDay(publisher, template, 1, "Day");
        addExercise(publisher, day, OFFICIAL_SQUAT_ID);
        addExercise(publisher, day, customExercise.toString());
        attachRoutine(publisher, day, routine);
        publish(publisher, template);

        String copier = registerAndToken();
        MvcResult copied = mockMvc.perform(post("/api/marketplace/templates/" + template + "/use")
                        .header("Authorization", "Bearer " + copier))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andReturn();
        UUID copyId = idOf(copied);
        JsonNode copyDetail = body(copied);

        // Copy is owned by the copier and editable; days/exercises/routines deep-copied.
        JsonNode exercises = copyDetail.get("days").get(0).get("exercises");
        assertThat(exercises).hasSize(2);
        JsonNode squat = findByField(exercises, "exerciseName", "Barbell Back Squat");
        JsonNode curl = findByExerciseNameContaining(exercises, "Publisher Curl");
        // Official reference kept; custom reference nulled — snapshots intact for both.
        assertThat(squat.get("exerciseId").asText()).isEqualTo(OFFICIAL_SQUAT_ID);
        assertThat(curl.get("exerciseId").isNull()).isTrue();
        assertThat(curl.get("exerciseType").asText()).isEqualTo("STRENGTH");
        assertThat(curl.get("muscleGroups").size()).isGreaterThan(0);
        // Routine copied as snapshot.
        assertThat(copyDetail.get("days").get(0).get("routines").get(0).get("routineContent").asText())
                .isEqualTo("Bike 5 min.");

        // The copy is fully startable from snapshots (custom exercise has null id but works).
        UUID gym = createGym(copier, uniqueName("Gym"));
        UUID copyDayId = UUID.fromString(copyDetail.get("days").get(0).get("id").asText());
        mockMvc.perform(post("/api/workouts/start")
                        .header("Authorization", "Bearer " + copier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "templateDayId", copyDayId.toString(), "gymId", gym.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises.length()").value(2));

        // uses_count incremented on the source.
        MvcResult sourceDetail = mockMvc.perform(get("/api/marketplace/templates/" + template)
                        .header("Authorization", "Bearer " + copier))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(body(sourceDetail).get("stats").get("uses").asLong()).isEqualTo(1);

        // Copy is independent: the copier can edit it; it is a private template of the copier.
        mockMvc.perform(get("/api/templates/" + copyId).header("Authorization", "Bearer " + copier))
                .andExpect(status().isOk());
    }

    @Test
    void cannotUseOrVotePrivateTemplate() throws Exception {
        String owner = registerAndToken();
        UUID privateTemplate = publishableTemplate(owner);
        String other = registerAndToken();

        mockMvc.perform(post("/api/marketplace/templates/" + privateTemplate + "/use")
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
        mockMvc.perform(vote(other, privateTemplate, "UP"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unpublishHidesFromMarketplace() throws Exception {
        String owner = registerAndToken();
        UUID template = publishableTemplate(owner);
        publish(owner, template);
        mockMvc.perform(post("/api/templates/" + template + "/unpublish").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));

        String viewer = registerAndToken();
        mockMvc.perform(get("/api/marketplace/templates/" + template).header("Authorization", "Bearer " + viewer))
                .andExpect(status().isNotFound());
    }

    @Test
    void marketplaceRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/marketplace/templates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // --- helpers ---

    private UUID publishableTemplate(String token) throws Exception {
        UUID template = createTemplate(token, uniqueName("Program"));
        UUID day = createDay(token, template, 1, "Day");
        addExercise(token, day, OFFICIAL_SQUAT_ID);
        return template;
    }

    private void publish(String token, UUID templateId) throws Exception {
        mockMvc.perform(post("/api/templates/" + templateId + "/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.RequestBuilder vote(String token, UUID templateId, String type)
            throws Exception {
        return post("/api/marketplace/templates/" + templateId + "/vote")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("voteType", type)));
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

    private void addExercise(String token, UUID dayId, String exerciseId) throws Exception {
        mockMvc.perform(post("/api/template-days/" + dayId + "/exercises")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "exerciseId", exerciseId, "plannedSets", 3, "plannedReps", "5"))))
                .andExpect(status().isCreated());
    }

    private void attachRoutine(String token, UUID dayId, UUID routineId) throws Exception {
        mockMvc.perform(post("/api/template-days/" + dayId + "/routines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("routineId", routineId.toString()))))
                .andExpect(status().isCreated());
    }

    private UUID createCustomExercise(String token, String name) throws Exception {
        return idOf(mockMvc.perform(post("/api/exercises/custom")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "exerciseType", "STRENGTH",
                                "muscleGroups", List.of(Map.of("code", "BICEPS", "role", "PRIMARY"))))))
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

    private UUID createGym(String token, String name) throws Exception {
        return idOf(mockMvc.perform(post("/api/gyms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
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

    private static boolean containsId(JsonNode array, UUID id) {
        return findByField(array, "id", id.toString()) != null;
    }

    private static JsonNode findByField(JsonNode array, String field, String value) {
        for (JsonNode element : array) {
            JsonNode candidate = element.get(field);
            if (candidate != null && candidate.asText().equals(value)) {
                return element;
            }
        }
        return null;
    }

    private static JsonNode findByExerciseNameContaining(JsonNode array, String fragment) {
        for (JsonNode element : array) {
            JsonNode name = element.get("exerciseName");
            if (name != null && name.asText().contains(fragment)) {
                return element;
            }
        }
        return null;
    }

    private static String uniqueName(String base) {
        return base + " " + UUID.randomUUID();
    }
}
