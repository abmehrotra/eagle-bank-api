package com.eaglebank.service;

import com.eaglebank.dto.UserRequest;
import com.eaglebank.dto.UserResponse;
import com.eaglebank.model.User;
import com.eaglebank.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public UserResponse createUser(UserRequest request) {
        if (repo.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPassword(new BCryptPasswordEncoder().encode(request.password()));

        User saved = repo.save(user);
        return new UserResponse(saved.getId(), saved.getFullName(), saved.getEmail());
    }

    public UserResponse getUserByIdForCurrentUser(Long userId) {
        User user = repo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        // Get email from SecurityContext
        String authenticatedEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // Ensure the requester is accessing their own data
        if (!user.getEmail().equals(authenticatedEmail)) {
            throw new AccessDeniedException("Access denied");
        }

        return new UserResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
