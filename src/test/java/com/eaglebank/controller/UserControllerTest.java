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


    @Test
    void createUser_ShouldReturn201Created() throws Exception {
        // Create a dummy existing user for JWT auth
        User authUser = new User();
        authUser.setFullName("Auth User");
        authUser.setEmail("auth@example.com");
        authUser.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(authUser);

        String token = jwtService.generateToken(authUser.getEmail());

        UserRequest newUser = new UserRequest("Jane Smith", "jane@example.com", "secure123");

        mockMvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())  // 201
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
        // Create and save user
        User user = new User();
        user.setFullName("Alice Johnson");
        user.setEmail("alice@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user = userRepository.save(user);

        // Generate JWT for this user
        String token = jwtService.generateToken(user.getEmail());

        mockMvc.perform(get("/v1/users/" + user.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Johnson"));
    }

    @Test
    void getUserByIdAccessDenied_ShouldReturn403Forbidden() throws Exception {
        // Target user in DB
        User targetUser = new User();
        targetUser.setFullName("Bob");
        targetUser.setEmail("bob@example.com");
        targetUser.setPassword(passwordEncoder.encode("password123"));
        targetUser = userRepository.save(targetUser);

        // Authenticated as another user
        User authUser = new User();
        authUser.setFullName("Alice");
        authUser.setEmail("alice@example.com");
        authUser.setPassword(passwordEncoder.encode("password456"));
        userRepository.save(authUser);

        String token = jwtService.generateToken(authUser.getEmail());

        mockMvc.perform(get("/v1/users/" + targetUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    void getUserByIdNotFound_ShouldReturn404() throws Exception {
        // Authenticated user
        User authUser = new User();
        authUser.setFullName("Alice");
        authUser.setEmail("alice@example.com");
        authUser.setPassword(passwordEncoder.encode("password456"));
        userRepository.save(authUser);

        String token = jwtService.generateToken(authUser.getEmail());

        // Non-existent userId
        long nonExistentUserId = 9999L;

        mockMvc.perform(get("/v1/users/" + nonExistentUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }
}
