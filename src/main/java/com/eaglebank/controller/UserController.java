package com.eaglebank.controller;

import com.eaglebank.dto.UserRequest;
import com.eaglebank.dto.UserResponse;
import com.eaglebank.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        log.info("Creating new user with email={}", request.email());

        UserResponse response = service.createUser(request);

        log.info("User created successfully. userId={} email={}", response.id(), response.email());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        log.info("Fetching user details for userId={}", userId);

        UserResponse response = service.getUserByIdForCurrentUser(userId);

        log.info("Retrieved user details. userId={} email={}", response.id(), response.email());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long userId, @Valid @RequestBody UserRequest request) {
        log.info("Updating user details. userId={} email={}", userId, request.email());

        UserResponse response = service.updateUserDetails(userId, request);

        log.info("User updated successfully. userId={} email={}", response.id(), response.email());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId) {
        log.info("Deleting user. userId={}", userId);

        service.deleteUser(userId);

        log.info("User deleted successfully. userId={}", userId);

        String response = "User with ID " + userId + " has been deleted.";
        return ResponseEntity.ok(response);
    }
}
