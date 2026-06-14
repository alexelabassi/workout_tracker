package com.thesis.workout.gym.web;

import static org.assertj.core.api.Assertions.assertThat;
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
class EquipmentFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createListUpdateDeleteEquipment() throws Exception {
        String token = registerAndToken();
        UUID gymId = createGym(token, uniqueName("Home Gym"));

        MvcResult created = mockMvc.perform(createEquipment(token, gymId, "Squat Rack", "MACHINE", "sturdy"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.gymId").value(gymId.toString()))
                .andExpect(jsonPath("$.name").value("Squat Rack"))
                .andExpect(jsonPath("$.equipmentType").value("MACHINE"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn();
        UUID equipmentId = idOf(created);

        MvcResult list = mockMvc.perform(
                        get("/api/gyms/" + gymId + "/equipment").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(findById(body(list), equipmentId)).isNotNull();

        mockMvc.perform(put("/api/equipment/" + equipmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Power Rack", "equipmentType", "OTHER", "notes", "updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Power Rack"))
                .andExpect(jsonPath("$.equipmentType").value("OTHER"));

        mockMvc.perform(delete("/api/equipment/" + equipmentId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        MvcResult afterDelete = mockMvc.perform(
                        get("/api/gyms/" + gymId + "/equipment").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(findById(body(afterDelete), equipmentId)).isNull();
    }

    @Test
    void nullableTypeIsAccepted() throws Exception {
        String token = registerAndToken();
        UUID gymId = createGym(token, uniqueName("Studio"));

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Yoga Mat");
        mockMvc.perform(post("/api/gyms/" + gymId + "/equipment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipmentType").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void duplicateNameInSameGymIsRejected() throws Exception {
        String token = registerAndToken();
        UUID gymId = createGym(token, uniqueName("Box"));

        mockMvc.perform(createEquipment(token, gymId, "Kettlebell", null, null)).andExpect(status().isCreated());
        mockMvc.perform(createEquipment(token, gymId, "kettlebell", null, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EQUIPMENT_NAME_TAKEN"));
    }

    @Test
    void sameNameAllowedInDifferentGyms() throws Exception {
        String token = registerAndToken();
        UUID gymA = createGym(token, uniqueName("Gym A"));
        UUID gymB = createGym(token, uniqueName("Gym B"));

        mockMvc.perform(createEquipment(token, gymA, "Treadmill", "CARDIO_MACHINE", null))
                .andExpect(status().isCreated());
        mockMvc.perform(createEquipment(token, gymB, "Treadmill", "CARDIO_MACHINE", null))
                .andExpect(status().isCreated());
    }

    @Test
    void softDeleteFreesTheName() throws Exception {
        String token = registerAndToken();
        UUID gymId = createGym(token, uniqueName("Garage"));
        UUID equipmentId = idOf(mockMvc.perform(createEquipment(token, gymId, "Bench", "BENCH", null)).andReturn());

        mockMvc.perform(delete("/api/equipment/" + equipmentId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(createEquipment(token, gymId, "Bench", "BENCH", null)).andExpect(status().isCreated());
    }

    @Test
    void cannotAddOrListEquipmentInAnotherUsersGym() throws Exception {
        String ownerToken = registerAndToken();
        String intruderToken = registerAndToken();
        UUID gymId = createGym(ownerToken, uniqueName("Owner Gym"));

        mockMvc.perform(createEquipment(intruderToken, gymId, "Sneaky", null, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("GYM_NOT_FOUND"));
        mockMvc.perform(get("/api/gyms/" + gymId + "/equipment").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("GYM_NOT_FOUND"));
    }

    @Test
    void anotherUsersEquipmentIsNotFound() throws Exception {
        String ownerToken = registerAndToken();
        String intruderToken = registerAndToken();
        UUID gymId = createGym(ownerToken, uniqueName("A Gym"));
        UUID equipmentId = idOf(mockMvc.perform(createEquipment(ownerToken, gymId, "Rower", null, null)).andReturn());

        mockMvc.perform(put("/api/equipment/" + equipmentId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hijack"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EQUIPMENT_NOT_FOUND"));
        mockMvc.perform(delete("/api/equipment/" + equipmentId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EQUIPMENT_NOT_FOUND"));
    }

    @Test
    void deletingGymCascadesSoftDeleteToEquipment() throws Exception {
        String token = registerAndToken();
        UUID gymId = createGym(token, uniqueName("Doomed Gym"));
        UUID equipmentId = idOf(mockMvc.perform(createEquipment(token, gymId, "Lat Pulldown", "CABLE", null))
                .andReturn());

        mockMvc.perform(delete("/api/gyms/" + gymId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Equipment was soft-deleted along with the gym: it is no longer addressable.
        mockMvc.perform(delete("/api/equipment/" + equipmentId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EQUIPMENT_NOT_FOUND"));

        // And its name is reusable in a fresh gym.
        UUID freshGym = createGym(token, uniqueName("Fresh Gym"));
        mockMvc.perform(createEquipment(token, freshGym, "Lat Pulldown", "CABLE", null))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidPayloadsFailValidation() throws Exception {
        String token = registerAndToken();
        UUID gymId = createGym(token, uniqueName("Validation Gym"));

        // Blank name.
        mockMvc.perform(createEquipment(token, gymId, "  ", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        // Bad enum value.
        mockMvc.perform(post("/api/gyms/" + gymId + "/equipment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "X", "equipmentType", "ROCKET"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        // Notes too long.
        mockMvc.perform(post("/api/gyms/" + gymId + "/equipment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "X", "notes", "n".repeat(5001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void equipmentEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/gyms/" + UUID.randomUUID() + "/equipment"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder createEquipment(
            String token, UUID gymId, String name, String type, String notes) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        if (type != null) {
            payload.put("equipmentType", type);
        }
        if (notes != null) {
            payload.put("notes", notes);
        }
        return post("/api/gyms/" + gymId + "/equipment")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload));
    }

    private UUID createGym(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/gyms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
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

    private static JsonNode findById(JsonNode array, UUID id) {
        for (JsonNode element : array) {
            JsonNode candidate = element.get("id");
            if (candidate != null && candidate.asText().equals(id.toString())) {
                return element;
            }
        }
        return null;
    }

    private static String uniqueName(String base) {
        return base + " " + UUID.randomUUID();
    }
}
