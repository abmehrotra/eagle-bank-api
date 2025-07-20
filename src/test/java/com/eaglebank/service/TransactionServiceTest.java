package com.eaglebank.service;

import com.eaglebank.dto.TransactionRequest;
import com.eaglebank.exception.InsufficientFundsException;
import com.eaglebank.model.BankAccount;
import com.eaglebank.model.Transaction;
import com.eaglebank.model.TransactionType;
import com.eaglebank.model.User;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.TransactionRepository;
import com.eaglebank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private BankAccountRepository accountRepo;
    @Mock
    private TransactionRepository transactionRepo;
    @Mock
    private UserRepository userRepo;
    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setupSecurityContext() {
        var auth = new UsernamePasswordAuthenticationToken("test@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testDepositTransaction_WithAccountAssociated_Returns200Success() {
        var user = new User(1L, "John", "pass", "test@example.com");
        var account = new BankAccount(1L, "SAVINGS", 100.0, user);
        var request = new TransactionRequest(50.0, TransactionType.DEPOSIT);

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepo.save(any())).thenAnswer(i -> {
            Transaction tx = i.getArgument(0);
            tx.setId(1L);
            return tx;
        });

        var response = transactionService.createTransaction(1L, request);

        assertEquals(150.0, response.updatedBalance());
        assertEquals(TransactionType.DEPOSIT, response.type());
        assertEquals(50.0, response.amount());
    }

    @Test
    void testWithdrawalTransaction_HavingSufficientBalance_Returns200Success() {
        var user = new User(1L, "John", "pass", "test@example.com");
        var account = new BankAccount(1L, "SAVINGS", 200.0, user);
        var request = new TransactionRequest(100.0, TransactionType.WITHDRAWAL);

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepo.save(any())).thenAnswer(i -> {
            Transaction tx = i.getArgument(0);
            tx.setId(2L);
            return tx;
        });

        var response = transactionService.createTransaction(1L, request);

        assertEquals(100.0, response.updatedBalance());
        assertEquals(TransactionType.WITHDRAWAL, response.type());
        assertEquals(100.0, response.amount());
    }

    @Test
    void testWithdrawalTransaction_HavingInsufficientBalance_Throws422UnprocessableEntity() {
        var user = new User(1L, "John", "pass", "test@example.com");
        var account = new BankAccount(1L, "SAVINGS", 30.0, user);
        var request = new TransactionRequest(100.0, TransactionType.WITHDRAWAL);

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(InsufficientFundsException.class,
                () -> transactionService.createTransaction(1L, request));
    }

    @Test
    void testCreateTransaction_ForAccountAssociatedWithAnotherUsersAccount_Returns403Forbidden() {
        var loggedInUser = new User(1L, "John", "pass", "john@example.com");
        var otherUser = new User(2L, "Jane", "pass", "jane@example.com");
        var account = new BankAccount(10L, "SAVINGS", 100.0, otherUser);
        var request = new TransactionRequest(50.0, TransactionType.DEPOSIT);

        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.of(loggedInUser));
        when(accountRepo.findById(10L)).thenReturn(Optional.of(account));

        var auth = new UsernamePasswordAuthenticationToken("john@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(AccessDeniedException.class,
                () -> transactionService.createTransaction(10L, request));
    }

    @Test
    void testCreateTransaction_ForAccountThatDoesNotExist_Returns404NotFound() {
        var user = new User(1L, "John", "pass", "john@example.com");
        var request = new TransactionRequest(50.0, TransactionType.DEPOSIT);

        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(accountRepo.findById(99L)).thenReturn(Optional.empty()); // Non-existent account

        var auth = new UsernamePasswordAuthenticationToken("john@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(NoSuchElementException.class, () -> transactionService.createTransaction(99L, request));
    }

    @Test
    void getTransaction_ReturnsCorrectResponse() {
        var user = new User(1L, "Alice", "pass", "alice@example.com");
        var account = new BankAccount(1L, "SAVINGS", 1000.0, user);
        var transaction = new Transaction(100.0, TransactionType.DEPOSIT, LocalDateTime.now(), account);
        transaction.setId(10L);

        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepo.findById(10L)).thenReturn(Optional.of(transaction));

        var auth = new UsernamePasswordAuthenticationToken("alice@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        var result = transactionService.getTransaction(1L, 10L);

        assertEquals(10L, result.transactionId());
        assertEquals(TransactionType.DEPOSIT, result.type());
        assertEquals(100.0, result.amount());
        assertEquals(1000.0, result.updatedBalance());
    }

    @Test
    void getTransaction_AccountNotOwnedByUser_Throws403Forbidden() {
        // Logged-in user
        var loggedInUser = new User(1L, "John", "pass", "john@example.com");

        // Account belongs to another user
        var otherUser = new User(2L, "Jane", "pass", "jane@example.com");
        var account = new BankAccount(20L, "SAVINGS", 500.0, otherUser);

        // Transaction belongs to that account
        var transaction = new Transaction(50.0, TransactionType.WITHDRAWAL, LocalDateTime.now(), account);
        transaction.setId(99L);

        // Mocking
        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.of(loggedInUser));
        when(accountRepo.findById(20L)).thenReturn(Optional.of(account));

        // Set the authenticated user
        var auth = new UsernamePasswordAuthenticationToken("john@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Assert
        assertThrows(AccessDeniedException.class,
                () -> transactionService.getTransaction(20L, 99L));
    }

    @Test
    void getTransaction_TransactionNotFound_Throws404NotFound() {
        var user = new User(1L, "Alice", "pass", "alice@example.com");
        var account = new BankAccount(1L, "SAVINGS", 1000.0, user);

        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepo.findById(10L)).thenReturn(Optional.empty());

        var auth = new UsernamePasswordAuthenticationToken("alice@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(NoSuchElementException.class, () -> transactionService.getTransaction(1L, 10L));
    }

    @Test
    void getTransaction_AccountDoesNotExist_Throws404NotFound() {
        var user = new User(1L, "Alice", "pass", "alice@example.com");

        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(accountRepo.findById(999L)).thenReturn(Optional.empty()); // Account not found

        var auth = new UsernamePasswordAuthenticationToken("alice@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(NoSuchElementException.class, () -> transactionService.getTransaction(999L, 10L));
    }

    @Test
    void getTransaction_TransactionNotLinkedToAccount_Throws404NotFound() {
        // Arrange
        var user = new User(1L, "Alice", "pass", "alice@example.com");

        var account = new BankAccount();
        account.setId(1L);
        account.setUser(user);

        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));

        // Create a transaction linked to a different account
        var differentAccount = new BankAccount();
        differentAccount.setId(2L);
        differentAccount.setUser(user);

        var transaction = new Transaction(100.0, TransactionType.DEPOSIT, LocalDateTime.now(), differentAccount);
        transaction.setId(10L);

        when(transactionRepo.findById(10L)).thenReturn(Optional.of(transaction));

        var auth = new UsernamePasswordAuthenticationToken("alice@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThrows(NoSuchElementException.class, () ->
                transactionService.getTransaction(1L, 10L));
    }
}

