package com.eaglebank.service;

import com.eaglebank.dto.UserRequest;
import com.eaglebank.dto.UserResponse;
import com.eaglebank.model.User;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.util.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class UserService {

    private final UserRepository repo;
    private final BankAccountRepository bankAccountRepository;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repo,
                       BankAccountRepository bankAccountRepository,
                       SecurityUtils securityUtils,
                       PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.bankAccountRepository = bankAccountRepository;
        this.securityUtils = securityUtils;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(UserRequest request) {
        if (repo.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User newUser = new User();
        newUser.setFullName(request.fullName());
        newUser.setEmail(request.email());
        newUser.setPassword(passwordEncoder.encode(request.password()));

        User savedUser = repo.save(newUser);
        return mapToResponse(savedUser);
    }

    public UserResponse getUserByIdForCurrentUser(Long userId) {
        User user = getUserOrThrow(userId);
        ensureCurrentUserAccess(user);
        return mapToResponse(user);
    }

    // --- Private utility methods ---

    private User getUserOrThrow(Long userId) {
        return repo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    private void ensureCurrentUserAccess(User user) {
        String authenticatedEmail = securityUtils.getAuthenticatedEmail();
        if (!user.getEmail().equals(authenticatedEmail)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
