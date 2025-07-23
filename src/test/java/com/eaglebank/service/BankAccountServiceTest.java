package com.eaglebank.service;

import com.eaglebank.dto.BankAccountRequest;
import com.eaglebank.dto.BankAccountResponse;
import com.eaglebank.model.BankAccount;
import com.eaglebank.model.User;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class BankAccountServiceTest {

    private BankAccountService service;
    private BankAccountRepository accountRepo;
    private UserRepository userRepo;

    @BeforeEach
    void setUp() {
        accountRepo = mock(BankAccountRepository.class);
        userRepo = mock(UserRepository.class);
        service = new BankAccountService(accountRepo, userRepo);
    }

    void setAuthContext(String email) {
        var auth = new UsernamePasswordAuthenticationToken(email, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    void mockStaticAuthContext(String email, Runnable assertions) {
        try (MockedStatic<SecurityContextHolder> contextMockedStatic = mockStatic(SecurityContextHolder.class)) {
            Authentication auth = mock(Authentication.class);
            SecurityContext context = mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(auth);
            when(auth.getName()).thenReturn(email);

            contextMockedStatic.when(SecurityContextHolder::getContext).thenReturn(context);

            assertions.run();
        }
    }

    @Test
    void testCreateAccount_WhenCurrentUserIsAuthenticated_ShouldReturn201Created() {
        BankAccountRequest request = new BankAccountRequest("SAVINGS", 1000.0);
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        BankAccount saved = new BankAccount();
        saved.setId(99L);
        saved.setAccountType("SAVINGS");
        saved.setBalance(1000.0);
        saved.setUser(user);

        mockStaticAuthContext("user@example.com", () -> {
            when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(accountRepo.save(any())).thenReturn(saved);

            BankAccountResponse response = service.createAccount(request);

            assertThat(response.id()).isEqualTo(99L);
            assertThat(response.accountType()).isEqualTo("SAVINGS");
            assertThat(response.balance()).isEqualTo(1000.0);
        });
    }

    @Test
    void testCreateAccount_WhenMissingRequiredData_ShouldThrowIllegalArgumentException() {
        BankAccountRequest invalidRequest = new BankAccountRequest(null, 1000.0); // missing accountType

        mockStaticAuthContext("user@example.com", () -> {
            User user = new User();
            user.setEmail("user@example.com");
            when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            assertThrows(IllegalArgumentException.class, () -> service.createAccount(invalidRequest));
        });
    }

    @Test
    void testCreateAccount_WhenUserNotFound_ShouldThrowIllegalArgumentException() {
        BankAccountRequest request = new BankAccountRequest("SAVINGS", 1000.0);

        mockStaticAuthContext("notfound@example.com", () -> {
            when(userRepo.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class, () -> service.createAccount(request));
        });
    }

    @Test
    void testGetAccountById_WhenCurrentUserIsAuthenticated_ShouldReturnAccountDetails() {
        String email = "owner@example.com";
        User user = new User(1L, "Owner", email, "pass");
        BankAccount account = new BankAccount(1L, "SAVINGS", 1000.0, user);

        setAuthContext(email);

        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));

        BankAccountResponse response = service.getAccountById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.accountType()).isEqualTo("SAVINGS");
        assertThat(response.balance()).isEqualTo(1000.0);

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetAccountById_WhenAccountDoesNotExist_ShouldThrowNoSuchElementException() {
        String email = "owner@example.com";
        User user = new User(1L, "Owner", email, "pass");

        setAuthContext(email);

        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));
        when(accountRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccountById(1L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Bank account not found");

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetAccountById_WhenCurrentUserIsNotAuthenticated_ShouldThrowAccessDeniedException() {
        User owner = new User(1L, "Owner", "owner@example.com", "pass");
        User otherUser = new User(2L, "Other", "other@example.com", "pass");
        BankAccount account = new BankAccount(1L, "SAVINGS", 999.0, owner);

        setAuthContext("other@example.com");

        when(userRepo.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.getAccountById(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied to this account");

        SecurityContextHolder.clearContext();
    }
}
