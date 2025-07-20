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
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("alice@example.com", null, null));
        SecurityContextHolder.setContext(context);
    }

    @Test
    void createUserSuccess() {
        UserRequest request = new UserRequest("Bob", "bob@example.com", "secret");

        when(repo.existsByEmail("bob@example.com")).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        var result = service.createUser(request);
        assertEquals("Bob", result.fullName());
    }

    @Test
    void createUserDuplicateEmailThrowsException() {
        when(repo.existsByEmail("bob@example.com")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () ->
                service.createUser(new UserRequest("Bob", "bob@example.com", "secret"))
        );
    }

    @Test
    void getUserByIdForCurrentUser_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");
        user.setFullName("Alice");
        user.setPassword("hashed");

        when(repo.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = service.getUserByIdForCurrentUser(1L);

        assertEquals(1L, response.id());
        assertEquals("Alice", response.fullName());
        assertEquals("alice@example.com", response.email());
    }

    @Test
    void getUserByIdForCurrentUser_AccessDenied() {
        User user = new User();
        user.setId(2L);
        user.setEmail("bob@example.com");
        user.setFullName("Bob");

        when(repo.findById(2L)).thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class, () -> service.getUserByIdForCurrentUser(2L));
    }

    @Test
    void getUserByIdForCurrentUser_UserNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getUserByIdForCurrentUser(99L));
    }
}
