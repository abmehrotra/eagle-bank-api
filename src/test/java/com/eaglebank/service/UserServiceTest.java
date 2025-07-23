package com.eaglebank.service;

import com.eaglebank.dto.UserRequest;
import com.eaglebank.dto.UserResponse;
import com.eaglebank.model.User;
import com.eaglebank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserServiceTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final UserService service = new UserService(repo);

    @BeforeEach
    void setupSecurityContext() {
        setAuthenticatedUser("alice@example.com");
    }

    private void setAuthenticatedUser(String email) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(email, null, null));
        SecurityContextHolder.setContext(context);
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
    }

    @Test
    void createUser_ShouldReturn404_DuplicateEmail() {
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
