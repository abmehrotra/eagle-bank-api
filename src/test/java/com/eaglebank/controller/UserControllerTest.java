package com.eaglebank.controller;

import com.eaglebank.dto.UserRequest;
import com.eaglebank.model.User;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    private User createUser(String fullName, String email, String rawPassword) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    private String generateTokenForUser(String email) {
        return jwtService.generateToken(email);
    }

    @Test
    void createUser_ShouldReturn201Created() throws Exception {
        User authUser = createUser("Auth User", "auth@example.com", "password123");
        String token = generateTokenForUser(authUser.getEmail());

        UserRequest newUser = new UserRequest("Jane Smith", "jane@example.com", "secure123");

        mockMvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void createUserMissingFields_ShouldReturn400BadRequest() throws Exception {
        UserRequest request = new UserRequest("", "invalid-email", "");

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getUserById_ShouldReturn200Success() throws Exception {
        User user = createUser("Alice Johnson", "alice@example.com", "password123");
        String token = generateTokenForUser(user.getEmail());

        mockMvc.perform(get("/v1/users/" + user.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Johnson"));
    }

    @Test
    void getUserByIdAccessDenied_ShouldReturn403Forbidden() throws Exception {
        User targetUser = createUser("Bob", "bob@example.com", "password123");
        User authUser = createUser("Alice", "alice@example.com", "password456");
        String token = generateTokenForUser(authUser.getEmail());

        mockMvc.perform(get("/v1/users/" + targetUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    void getUserByIdNotFound_ShouldReturn404NotFound() throws Exception {
        User authUser = createUser("Alice", "alice@example.com", "password456");
        String token = generateTokenForUser(authUser.getEmail());

        mockMvc.perform(get("/v1/users/9999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }
}