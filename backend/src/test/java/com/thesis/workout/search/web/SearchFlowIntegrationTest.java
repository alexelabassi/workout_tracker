package com.thesis.workout.search.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.workout.AbstractSearchIntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end tests for the OpenSearch-backed search/read model against a real OpenSearch container.
 * Exercises after-commit indexing, boosted full-text, typo tolerance, synonyms, structured filters,
 * facets, highlighting, owner/visibility security, the defense-in-depth PostgreSQL post-filter, and
 * the admin-only reindex.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SearchFlowIntegrationTest extends AbstractSearchIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret!";
    private static final String OFFICIAL_SQUAT_ID = "00000000-0000-0000-0000-000000000101";
    private static final String OFFICIAL_BENCH_ID = "00000000-0000-0000-0000-000000000103";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void templateMyScopeFullTextMatchesNameAndExerciseAndCarriesAnalyzerFields() throws Exception {
        String owner = registerAndToken();
        String token = uniqueToken();
        UUID template = templateWithBench(owner, "Hypertrophy Bench Blast " + token);

        JsonNode result = searchTemplates(owner, "scope=my&q=bench");
        JsonNode item = findById(result.get("items"), template);
        assertThat(item).isNotNull();
        // Analyzer-derived structural fields are indexed on a structural reindex.
        assertThat(item.get("templateStructureScore").isNull()).isFalse();
        assertThat(item.get("analysisCategory").isNull()).isFalse();
        // Highlighting marks the matched term.
        assertThat(item.get("highlights").toString()).contains("<mark>");
        assertThat(item.get("exerciseNames").toString()).contains("Bench");
    }

    @Test
    void templateSearchIsTypoTolerant() throws Exception {
        String owner = registerAndToken();
        String token = uniqueToken();
        UUID template = templateWithBench(owner, "Strength " + token);

        // "benhc" is one transposition away from "bench" — fuzziness=AUTO should still match.
        JsonNode result = searchTemplates(owner, "scope=my&q=benhc");
        assertThat(findById(result.get("items"), template)).isNotNull();
    }

    @Test
    void templateSearchExpandsGymSynonyms() throws Exception {
        String owner = registerAndToken();
        String token = uniqueToken();
        // Name/exercise carry no "chest" text; the only match path is muscleGroupsText (CHEST) via
        // the "pecs -> chest" synonym applied at search time.
        UUID template = templateWithBench(owner, "Probe " + token);

        JsonNode result = searchTemplates(owner, "scope=my&q=pecs");
        assertThat(findById(result.get("items"), template)).isNotNull();
    }

    @Test
    void marketplaceScopeOnlyMatchesPublicTemplatesAndIsolatesPrivate() throws Exception {
        String token = uniqueToken();
        String publisher = registerAndToken();
        UUID published = templateWithBench(publisher, "Marketplace " + token);
        publish(publisher, published);

        String other = registerAndToken();
        UUID privateOne = templateWithBench(other, "Marketplace " + token); // same token, but PRIVATE

        String viewer = registerAndToken();
        JsonNode result = searchTemplates(viewer, "scope=marketplace&q=" + token);
        assertThat(findById(result.get("items"), published)).isNotNull();
        assertThat(findById(result.get("items"), privateOne)).isNull();
        // The marketplace result carries the author name merged from PostgreSQL.
        assertThat(findById(result.get("items"), published).get("authorDisplayName").asText()).isNotBlank();
    }

    @Test
    void unpublishRemovesTemplateFromMarketplaceSearch() throws Exception {
        String token = uniqueToken();
        String publisher = registerAndToken();
        UUID template = templateWithBench(publisher, "Toggle " + token);
        publish(publisher, template);

        String viewer = registerAndToken();
        assertThat(findById(searchTemplates(viewer, "scope=marketplace&q=" + token).get("items"), template))
                .isNotNull();

        mockMvc.perform(post("/api/templates/" + template + "/unpublish")
                        .header("Authorization", "Bearer " + publisher))
                .andExpect(status().isOk());

        assertThat(findById(searchTemplates(viewer, "scope=marketplace&q=" + token).get("items"), template))
                .isNull();
    }

    @Test
    void templateFilterAndFacetsAreReturned() throws Exception {
        String owner = registerAndToken();
        String token = uniqueToken();
        templateWithBench(owner, "Faceted " + token); // INTERMEDIATE default difficulty unset -> null

        JsonNode result = searchTemplates(owner, "scope=my&q=" + "Faceted");
        // Facets are always present (difficulty, splitType, daysPerWeek, muscleGroups, analysisCategory).
        List<String> facetFields = facetFields(result.get("facets"));
        assertThat(facetFields).contains("difficulty", "splitType", "muscleGroups", "analysisCategory");
    }

    @Test
    void workoutSearchIsOwnerScopedAndMatchesSnapshots() throws Exception {
        String token = uniqueToken();
        String athlete = registerAndToken();
        finishedWorkout(athlete, "Gym " + token);

        JsonNode result = searchWorkouts(athlete, "q=bench");
        assertThat(result.get("items").size()).isGreaterThanOrEqualTo(1);
        JsonNode first = result.get("items").get(0);
        assertThat(first.get("exerciseNameSnapshots").toString()).contains("Bench");
        // Owner-scoped facets present (status terms + month date-histogram).
        assertThat(facetFields(result.get("facets"))).contains("status", "byMonth");

        // A different user must never see another athlete's history.
        String intruder = registerAndToken();
        JsonNode intruderResult = searchWorkouts(intruder, "q=bench&gym=Gym " + token);
        assertThat(intruderResult.get("items").size()).isEqualTo(0);
    }

    @Test
    void workoutSearchAppliesStructuredFilters() throws Exception {
        String athlete = registerAndToken();
        finishedWorkout(athlete, "Filterable Gym");

        // Status filter keeps FINISHED sessions; an impossible volume floor removes everything.
        assertThat(searchWorkouts(athlete, "status=FINISHED").get("items").size()).isGreaterThanOrEqualTo(1);
        assertThat(searchWorkouts(athlete, "minVolume=999999999").get("items").size()).isEqualTo(0);
    }

    @Test
    void reindexEndpointIsForbiddenForNonAdminUsers() throws Exception {
        String user = registerAndToken();
        mockMvc.perform(post("/api/admin/search/reindex").header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/search/templates?scope=marketplace"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/search/workouts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void coachSearchesActiveClientWorkoutsGatedByRelationship() throws Exception {
        Account coach = coachAccount();
        Account client = registerAccount();
        String gymToken = uniqueToken();
        finishedWorkout(client.token(), "Coach Gym " + gymToken);
        activeRelationship(coach, client);

        // The coach reuses the workout search with the CLIENT's owner id, but only after passing the
        // ACTIVE-relationship gate. The defense-in-depth PostgreSQL post-filter is scoped to the client.
        JsonNode result = body(mockMvc.perform(
                        get("/api/coach/clients/" + client.id() + "/search/workouts?q=bench")
                                .header("Authorization", "Bearer " + coach.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(result.get("items").size()).isGreaterThanOrEqualTo(1);
        assertThat(result.get("items").get(0).get("exerciseNameSnapshots").toString()).contains("Bench");

        // A coach with no ACTIVE relationship to a stranger is blocked by the gate -> 404 (IDOR-safe).
        Account stranger = registerAccount();
        mockMvc.perform(get("/api/coach/clients/" + stranger.id() + "/search/workouts?q=bench")
                        .header("Authorization", "Bearer " + coach.token()))
                .andExpect(status().isNotFound());

        // A non-coach user is rejected by RBAC before the gate even runs -> 403.
        mockMvc.perform(get("/api/coach/clients/" + client.id() + "/search/workouts?q=bench")
                        .header("Authorization", "Bearer " + client.token()))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private record Account(UUID id, String email, String token) {
    }

    /** Registers a fresh user and captures id + email + token. */
    private Account registerAccount() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        JsonNode body = body(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", PASSWORD, "displayName", "Search Tester"))))
                .andExpect(status().isCreated())
                .andReturn());
        return new Account(
                UUID.fromString(body.get("user").get("id").asText()), email, body.get("accessToken").asText());
    }

    /** Registers a user, promotes them to COACH in the DB, and re-logs in for a ROLE_COACH token. */
    private Account coachAccount() throws Exception {
        Account account = registerAccount();
        jdbcTemplate.update("UPDATE app_users SET role = 'COACH' WHERE id = ?", account.id());
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", account.email(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return new Account(account.id(), account.email(), body(login).get("accessToken").asText());
    }

    /** Coach invites the client; the client accepts; the relationship is now ACTIVE. */
    private void activeRelationship(Account coach, Account client) throws Exception {
        mockMvc.perform(post("/api/coach/clients/invite")
                        .header("Authorization", "Bearer " + coach.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("clientEmail", client.email()))))
                .andExpect(status().isCreated());
        JsonNode invites = body(mockMvc.perform(get("/api/coaching/invites")
                        .header("Authorization", "Bearer " + client.token()))
                .andExpect(status().isOk())
                .andReturn());
        UUID relationshipId = UUID.fromString(invites.get(0).get("relationshipId").asText());
        mockMvc.perform(post("/api/coaching/invites/" + relationshipId + "/accept")
                        .header("Authorization", "Bearer " + client.token()))
                .andExpect(status().isNoContent());
    }

    private JsonNode searchTemplates(String token, String query) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/search/templates?" + query)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return body(result);
    }

    private JsonNode searchWorkouts(String token, String query) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/search/workouts?" + query)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return body(result);
    }

    /** Creates a private template with one day and the official bench press (CHEST primary). */
    private UUID templateWithBench(String token, String name) throws Exception {
        UUID template = createTemplate(token, name);
        UUID day = createDay(token, template, 1, "Day A");
        addExercise(token, day, OFFICIAL_BENCH_ID);
        return template;
    }

    /** Runs a full workout (start, log a bench set, finish) so it lands in the workout index. */
    private void finishedWorkout(String token, String gymName) throws Exception {
        UUID template = createTemplate(token, "Session " + UUID.randomUUID());
        UUID day = createDay(token, template, 1, "Day A");
        addExercise(token, day, OFFICIAL_BENCH_ID);
        UUID gym = createGym(token, gymName);

        MvcResult started = mockMvc.perform(post("/api/workouts/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "templateDayId", day.toString(), "gymId", gym.toString()))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode detail = body(started);
        UUID sessionId = UUID.fromString(detail.get("id").asText());
        UUID sessionExerciseId = UUID.fromString(detail.get("exercises").get(0).get("id").asText());

        mockMvc.perform(post("/api/session-exercises/" + sessionExerciseId + "/sets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "setType", "WORKING", "weight", 60, "reps", 8))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/workouts/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void publish(String token, UUID templateId) throws Exception {
        mockMvc.perform(post("/api/templates/" + templateId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
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
                                "exerciseId", exerciseId, "plannedSets", 4, "plannedReps", "8"))))
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

    private String registerAndToken() throws Exception {
        return registerAccount().token();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UUID idOf(MvcResult result) throws Exception {
        return UUID.fromString(body(result).get("id").asText());
    }

    private static JsonNode findById(JsonNode items, UUID id) {
        for (JsonNode element : items) {
            JsonNode candidate = element.get("templateId");
            if (candidate != null && candidate.asText().equals(id.toString())) {
                return element;
            }
        }
        return null;
    }

    private static List<String> facetFields(JsonNode facets) {
        List<String> fields = new java.util.ArrayList<>();
        for (JsonNode facet : facets) {
            fields.add(facet.get("field").asText());
        }
        return fields;
    }

    private static String uniqueToken() {
        return "tok" + UUID.randomUUID().toString().replace("-", "");
    }
}
