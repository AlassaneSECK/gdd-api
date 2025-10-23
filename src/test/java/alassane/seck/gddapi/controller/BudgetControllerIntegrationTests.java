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
    void shouldCreateBudgetFromIncomeEntries() throws Exception {
        String email = "budgeter@example.com";
        String token = register(email, "password123");
        Long userId = userRepository.findByEmail(email).getId();

        Instant firstIncomeAt = Instant.now().minusSeconds(600);
        Instant secondIncomeAt = Instant.now();

        // Deux revenus successifs doivent cumuler le solde sans action manuelle sur /api/budget.
        createEntry(token, new EntryRequest("INCOME", BigDecimal.valueOf(1000), firstIncomeAt, "Salaire"));
        createEntry(token, new EntryRequest("INCOME", BigDecimal.valueOf(500), secondIncomeAt, "Prime"));

        mockMvc.perform(get("/api/budget")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.availableAmount").value(1500));

        mockMvc.perform(get("/api/budget/entries")
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
    void shouldDecreaseBudgetWhenExpenseRecorded() throws Exception {
        String email = "entries@example.com";
        String token = register(email, "password123");

        Instant incomeAt = Instant.now().minusSeconds(120);
        Instant expenseAt = Instant.now();

        // On crédite le budget...
        createEntry(token, new EntryRequest("INCOME", BigDecimal.valueOf(1000), incomeAt, "Salaire"));

        // ...puis on débite via une dépense : la réponse doit contenir le solde recalculé (800).
        mockMvc.perform(post("/api/budget/entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(new EntryRequest("EXPENSE", BigDecimal.valueOf(200), expenseAt, "Courses"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entry.type").value("EXPENSE"))
                .andExpect(jsonPath("$.entry.amount").value(200))
                .andExpect(jsonPath("$.entry.description").value("Courses"))
                .andExpect(jsonPath("$.budget.availableAmount").value(800));

        mockMvc.perform(get("/api/budget/entries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$.content[0].amount").value(200))
                .andExpect(jsonPath("$.content[0].description").value("Courses"))
                .andExpect(jsonPath("$.content[1].type").value("INCOME"))
                .andExpect(jsonPath("$.content[1].amount").value(1000))
                .andExpect(jsonPath("$.content[1].description").value("Salaire"));

        mockMvc.perform(get("/api/budget")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableAmount").value(800));
    }

    @Test
    void shouldReturnNotFoundWhenBudgetDoesNotExistYet() throws Exception {
        String email = "missing@example.com";
        String token = register(email, "password123");

        mockMvc.perform(get("/api/budget")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private void createEntry(String token, EntryRequest request) throws Exception {
        // Utilitaire : chaque appel valide que l'API retourne bien 201 et déclenche la mise à jour du budget.
        mockMvc.perform(post("/api/budget/entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(request)))
                .andExpect(status().isCreated());
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

    private record EntryRequest(String type, BigDecimal amount, Instant occurredAt, String description) {}
}
