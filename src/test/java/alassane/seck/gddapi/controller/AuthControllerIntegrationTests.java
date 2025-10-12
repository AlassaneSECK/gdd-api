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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTests {

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
    void registerShouldCreateUserAndReturnToken() throws Exception {
        String payload = objectMapper.writeValueAsString(new AuthRequestPayload("john@example.com", "password"));

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.path("token").asText()).isNotBlank();
        assertThat(userRepository.findByEmail("john@example.com")).isNotNull();
    }

    @Test
    void loginShouldAuthenticateExistingUser() throws Exception {
        register("sara@example.com", "top-secret");

        String payload = objectMapper.writeValueAsString(new AuthRequestPayload("sara@example.com", "top-secret"));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.path("token").asText()).isNotBlank();
    }

    @Test
    void loginShouldFailWithInvalidCredentials() throws Exception {
        register("emma@example.com", "strong-pass");

        String payload = objectMapper.writeValueAsString(new AuthRequestPayload("emma@example.com", "wrongpass"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerShouldRejectExistingEmail() throws Exception {
        register("louis@example.com", "old-pass");

        String payload = objectMapper.writeValueAsString(new AuthRequestPayload("louis@example.com", "new-pass"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void registerShouldRejectInvalidEmail() throws Exception {
        String payload = objectMapper.writeValueAsString(new AuthRequestPayload("not-an-email", "password"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    private void register(String email, String password) throws Exception {
        String payload = objectMapper.writeValueAsString(new AuthRequestPayload(email, password));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private record AuthRequestPayload(String email, String password) {}
}
