package alassane.seck.gddapi.controller;

import alassane.seck.gddapi.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BudgetControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateBudgetOnTheFlyAndApplyDelta() throws Exception {
        String email = "budgeter@example.com";
        String token = register(email, "password123");
        Long userId = userRepository.findByEmail(email).getId();

        applyDelta(userId, token, BigDecimal.valueOf(1000), "Salaire");
        mockMvc.perform(patch("/api/users/{userId}/budget", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(new DeltaRequest(BigDecimal.valueOf(500), "Prime"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableAmount").value(1500));

        mockMvc.perform(get("/api/users/{userId}/budget", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableAmount").value(1500));

        mockMvc.perform(get("/api/users/{userId}/budget/entries", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].type").value("INCOME"))
                .andExpect(jsonPath("$.content[0].amount").value(500))
                .andExpect(jsonPath("$.content[0].description").value("Prime"))
                .andExpect(jsonPath("$.content[1].type").value("INCOME"))
                .andExpect(jsonPath("$.content[1].amount").value(1000))
                .andExpect(jsonPath("$.content[1].description").value("Salaire"));
    }

    @Test
    void shouldOverrideBudgetWithPut() throws Exception {
        String email = "setter@example.com";
        String token = register(email, "password123");
        Long userId = userRepository.findByEmail(email).getId();

        applyDelta(userId, token, BigDecimal.valueOf(250), "Allocation initiale");

        mockMvc.perform(put("/api/users/{userId}/budget", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(new AmountRequest(BigDecimal.valueOf(2000)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableAmount").value(2000));
    }

    @Test
    void shouldReturnNotFoundWhenBudgetDoesNotExistYet() throws Exception {
        String email = "missing@example.com";
        String token = register(email, "password123");
        Long userId = userRepository.findByEmail(email).getId();

        mockMvc.perform(get("/api/users/{userId}/budget", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRecordManualEntryAndReflectInBudget() throws Exception {
        String email = "entries@example.com";
        String token = register(email, "password123");
        Long userId = userRepository.findByEmail(email).getId();

        Instant occurredAt = Instant.now().plusSeconds(3600);

        mockMvc.perform(patch("/api/users/{userId}/budget", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(new DeltaRequest(BigDecimal.valueOf(1000), "Salaire"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/{userId}/budget/entries", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(new EntryRequest("EXPENSE", BigDecimal.valueOf(200), occurredAt, "Courses"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entry.type").value("EXPENSE"))
                .andExpect(jsonPath("$.entry.amount").value(200))
                .andExpect(jsonPath("$.entry.description").value("Courses"))
                .andExpect(jsonPath("$.budget.availableAmount").value(800));

        mockMvc.perform(get("/api/users/{userId}/budget/entries", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$.content[0].amount").value(200))
                .andExpect(jsonPath("$.content[0].description").value("Courses"))
                .andExpect(jsonPath("$.content[1].type").value("INCOME"))
                .andExpect(jsonPath("$.content[1].amount").value(1000))
                .andExpect(jsonPath("$.content[1].description").value("Salaire"));
    }

    private void applyDelta(Long userId, String token, BigDecimal delta, String description) throws Exception {
        mockMvc.perform(patch("/api/users/{userId}/budget", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(new DeltaRequest(delta, description))))
                .andExpect(status().isOk());
    }

    private String register(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(new AuthRequestPayload(email, password))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        String token = json.path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private String payload(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    private record AuthRequestPayload(String email, String password) {}

    private record DeltaRequest(BigDecimal delta, String description) {}

    private record AmountRequest(BigDecimal amount) {}

    private record EntryRequest(String type, BigDecimal amount, Instant occurredAt, String description) {}
}
