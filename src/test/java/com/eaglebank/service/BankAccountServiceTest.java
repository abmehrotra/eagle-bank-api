package com.eaglebank.service;

import com.eaglebank.dto.BankAccountRequest;
import com.eaglebank.dto.BankAccountResponse;
import com.eaglebank.model.BankAccount;
import com.eaglebank.model.User;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


class BankAccountServiceTest {

    private BankAccountService service;
    private BankAccountRepository accountRepo;
    private UserRepository userRepo;
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        accountRepo = mock(BankAccountRepository.class);
        userRepo = mock(UserRepository.class);
        securityUtils = mock(SecurityUtils.class);
        service = new BankAccountService(accountRepo, userRepo, securityUtils);
    }

    @Test
    void testCreateAccount_WhenCurrentUserIsAuthenticated_ShouldReturn201Created() {
        BankAccountRequest request = new BankAccountRequest("SAVINGS", 1000.0);
        User user = new User(1L, "Test", "user@example.com", "pass");

        BankAccount saved = new BankAccount(99L, "SAVINGS", 1000.0, user);

        when(securityUtils.getAuthenticatedUser()).thenReturn(user);
        when(accountRepo.save(any())).thenReturn(saved);

        BankAccountResponse response = service.createAccount(request);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.accountType()).isEqualTo("SAVINGS");
        assertThat(response.balance()).isEqualTo(1000.0);
    }

    @Test
    void testCreateAccount_WhenUserNotFound_ShouldThrow400_IllegalArgumentException() {
        when(securityUtils.getAuthenticatedUser()).thenThrow(new IllegalArgumentException("User not found"));

        BankAccountRequest request = new BankAccountRequest("SAVINGS", 1000.0);

        assertThatThrownBy(() -> service.createAccount(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void testGetAccountById_WhenCurrentUserIsAuthenticated_ShouldReturnAccountDetails() {
        User user = new User(1L, "Owner", "owner@example.com", "pass");
        BankAccount account = new BankAccount(1L, "SAVINGS", 1000.0, user);

        when(securityUtils.getAuthenticatedUser()).thenReturn(user);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));

        BankAccountResponse response = service.getAccountById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.accountType()).isEqualTo("SAVINGS");
        assertThat(response.balance()).isEqualTo(1000.0);
    }

    @Test
    void testGetAccountById_WhenAccountDoesNotExist_ShouldThrow404_AccountNotFound() {
        User user = new User(1L, "Owner", "owner@example.com", "pass");

        when(securityUtils.getAuthenticatedUser()).thenReturn(user);
        when(accountRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccountById(1L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Bank account not found");
    }

    @Test
    void testGetAccountById_WhenCurrentUserIsNotAuthenticated_ShouldThrow403_Forbidden() {
        User owner = new User(1L, "Owner", "owner@example.com", "pass");
        User intruder = new User(2L, "Intruder", "intruder@example.com", "pass");
        BankAccount account = new BankAccount(1L, "SAVINGS", 999.0, owner);

        when(securityUtils.getAuthenticatedUser()).thenReturn(intruder);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.getAccountById(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }
}
