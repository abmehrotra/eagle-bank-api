package com.eaglebank.controller;

import com.eaglebank.dto.UserLoginRequest;
import com.eaglebank.dto.UserLoginResponse;
import com.eaglebank.model.User;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.security.JwtService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        log.info("Login attempt for email={}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found email={}", request.email());
                    return new IllegalArgumentException("Invalid credentials");
                });

        if (!encoder.matches(request.password(), user.getPassword())) {
            log.warn("Login failed: invalid password for email={}", request.email());
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());
        log.info("Login successful for email={}", request.email());

        return ResponseEntity.ok(new UserLoginResponse(token));
    }
}
