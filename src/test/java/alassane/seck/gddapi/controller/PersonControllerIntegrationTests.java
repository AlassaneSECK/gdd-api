package alassane.seck.gddapi.controller;

import alassane.seck.gddapi.repository.PersonRepository;
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

import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        personRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRejectRequestsWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/persons"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateAndRetrievePersonWithJwt() throws Exception {
        String token = registerAndGetToken();

        String payload = objectMapper.writeValueAsString(new PersonPayload("Alice", "Paris", "+33123456789"));

        String createdBody = mockMvc.perform(post("/api/persons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdJson = objectMapper.readTree(createdBody);
        assertThat(createdJson.path("id").asLong()).isPositive();

        String listBody = mockMvc.perform(get("/api/persons")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode listJson = objectMapper.readTree(listBody);
        assertThat(listJson.isArray()).isTrue();
        boolean found = StreamSupport.stream(listJson.spliterator(), false)
                .anyMatch(node -> "Alice".equals(node.path("name").asText()));
        assertThat(found).isTrue();
    }

    private String registerAndGetToken() throws Exception {
        String username = "user" + UUID.randomUUID();
        String password = "pass" + UUID.randomUUID();
        String payload = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.path("token").asText();
    }

    private record PersonPayload(String name, String city, String phoneNumber) {}
}

