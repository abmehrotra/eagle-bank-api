package com.eaglebank.service;

import com.eaglebank.dto.UserRequest;
import com.eaglebank.dto.UserResponse;
import com.eaglebank.model.User;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UserRepository repo;
    private BankAccountRepository bankRepository;
    private SecurityUtils securityUtils;
    private PasswordEncoder passwordEncoder;
    private UserService service;

    @BeforeEach
    void setup() {
        repo = mock(UserRepository.class);
        bankRepository = mock(BankAccountRepository.class);
        securityUtils = mock(SecurityUtils.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserService(repo, bankRepository, securityUtils, passwordEncoder);

        when(securityUtils.getAuthenticatedEmail()).thenReturn("alice@example.com");
        when(passwordEncoder.encode(any())).thenAnswer(inv -> "encoded-" + inv.getArgument(0));
    }

    private User user(Long id, String name, String email, String password) {
        User u = new User();
        u.setId(id);
        u.setFullName(name);
        u.setEmail(email);
        u.setPassword(password);
        return u;
    }

    private UserRequest userRequest(String name, String email, String password) {
        return new UserRequest(name, email, password);
    }

    @Test
    void createUser_ShouldReturn201_SuccessfulUserCreation() {
        UserRequest request = userRequest("Bob", "bob@example.com", "secret");

        when(repo.existsByEmail(request.email())).thenReturn(false);
        when(repo.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse result = service.createUser(request);
        assertEquals("Bob", result.fullName());
        assertEquals("bob@example.com", result.email());
    }

    @Test
    void createUser_WhenEmailExists_ShouldReturn400_BadRequest() {
        when(repo.existsByEmail("bob@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.createUser(userRequest("Bob", "bob@example.com", "secret")));
    }

    @Test
    void getUserByIdForCurrentUser_ShouldReturnUserResponse_AfterSuccessfulCreation() {
        User user = user(1L, "Alice", "alice@example.com", "hashed");

        when(repo.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = service.getUserByIdForCurrentUser(1L);

        assertEquals(1L, response.id());
        assertEquals("Alice", response.fullName());
        assertEquals("alice@example.com", response.email());
    }

    @Test
    void getUserByIdForCurrentUser_ShouldReturn403_Forbidden() {
        User user = user(2L, "Bob", "bob@example.com", "hashed");

        when(repo.findById(2L)).thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class, () -> service.getUserByIdForCurrentUser(2L));
    }

    @Test
    void getUserByIdForCurrentUser_ShouldReturn404_UserNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getUserByIdForCurrentUser(99L));
    }
}
