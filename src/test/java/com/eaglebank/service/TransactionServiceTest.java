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
        authenticateAs("test@example.com");
    }

    private void authenticateAs(String email) {
        var auth = new UsernamePasswordAuthenticationToken(email, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private User createUser(Long id, String email) {
        return new User(id, "Name", "pass", email);
    }

    private BankAccount createAccount(Long id, double balance, User owner) {
        return new BankAccount(id, "SAVINGS", balance, owner);
    }

    @Test
    void testDepositTransaction_WithAccountAssociated_Returns200_Success() {
        var user = createUser(1L, "test@example.com");
        var account = createAccount(1L, 100.0, user);
        var request = new TransactionRequest(50.0, TransactionType.DEPOSIT);

        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
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
    void testWithdrawalTransaction_HavingSufficientBalance_Returns200_Success() {
        var user = createUser(1L, "test@example.com");
        var account = createAccount(1L, 200.0, user);
        var request = new TransactionRequest(100.0, TransactionType.WITHDRAWAL);

        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
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
    void testWithdrawalTransaction_HavingInsufficientBalance_ShouldThrow422_UnprocessableEntity() {
        var user = createUser(1L, "test@example.com");
        var account = createAccount(1L, 30.0, user);
        var request = new TransactionRequest(100.0, TransactionType.WITHDRAWAL);

        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(InsufficientFundsException.class,
                () -> transactionService.createTransaction(1L, request));
    }

    @Test
    void testCreateTransaction_AnotherUsersAccount_Returns403_Forbidden() {
        var loggedInUser = createUser(1L, "john@example.com");
        var otherUser = createUser(2L, "jane@example.com");
        var account = createAccount(10L, 100.0, otherUser);
        var request = new TransactionRequest(50.0, TransactionType.DEPOSIT);

        authenticateAs(loggedInUser.getEmail());

        when(userRepo.findByEmail(loggedInUser.getEmail())).thenReturn(Optional.of(loggedInUser));
        when(accountRepo.findById(10L)).thenReturn(Optional.of(account));

        assertThrows(AccessDeniedException.class,
                () -> transactionService.createTransaction(10L, request));
    }

    @Test
    void testCreateTransaction_AccountDoesNotExist_Returns404_NotFound() {
        var user = createUser(1L, "john@example.com");
        var request = new TransactionRequest(50.0, TransactionType.DEPOSIT);

        authenticateAs(user.getEmail());

        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> transactionService.createTransaction(99L, request));
    }

    @Test
    void getTransaction_ReturnsCorrectResponse() {
        var user = createUser(1L, "alice@example.com");
        var account = createAccount(1L, 1000.0, user);
        var transaction = new Transaction(100.0, TransactionType.DEPOSIT, LocalDateTime.now(), account);
        transaction.setId(10L);

        authenticateAs(user.getEmail());

        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepo.findById(account.getId())).thenReturn(Optional.of(account));
        when(transactionRepo.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        var result = transactionService.getTransaction(1L, 10L);

        assertEquals(10L, result.transactionId());
        assertEquals(TransactionType.DEPOSIT, result.type());
        assertEquals(100.0, result.amount());
        assertEquals(1000.0, result.updatedBalance());
    }

    @Test
    void getTransaction_AccountNotOwnedByUser_ShouldThrow403_Forbidden() {
        var loggedInUser = createUser(1L, "john@example.com");
        var otherUser = createUser(2L, "jane@example.com");
        var account = createAccount(20L, 500.0, otherUser);
        var transaction = new Transaction(50.0, TransactionType.WITHDRAWAL, LocalDateTime.now(), account);
        transaction.setId(99L);

        authenticateAs(loggedInUser.getEmail());

        when(userRepo.findByEmail(loggedInUser.getEmail())).thenReturn(Optional.of(loggedInUser));
        when(accountRepo.findById(account.getId())).thenReturn(Optional.of(account));
        when(transactionRepo.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        assertThrows(AccessDeniedException.class,
                () -> transactionService.getTransaction(20L, 99L));
    }

    @Test
    void getTransaction_TransactionNotFound_ShouldThrow404_NotFound() {
        var user = createUser(1L, "alice@example.com");
        var account = createAccount(1L, 1000.0, user);

        authenticateAs(user.getEmail());

        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepo.findById(account.getId())).thenReturn(Optional.of(account));
        when(transactionRepo.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> transactionService.getTransaction(1L, 10L));
    }

    @Test
    void getTransaction_AccountDoesNotExist_ShouldThrow404_NotFound() {
        var user = createUser(1L, "alice@example.com");

        authenticateAs(user.getEmail());

        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> transactionService.getTransaction(999L, 10L));
    }

    @Test
    void getTransaction_TransactionForInAccount_ShouldThrow404_NotFound() {
        var user = createUser(1L, "alice@example.com");
        var account = createAccount(1L, 0.0, user);
        var differentAccount = createAccount(2L, 0.0, user);

        var transaction = new Transaction(100.0, TransactionType.DEPOSIT, LocalDateTime.now(), differentAccount);
        transaction.setId(10L);

        authenticateAs(user.getEmail());

        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepo.findById(account.getId())).thenReturn(Optional.of(account));
        when(transactionRepo.findById(10L)).thenReturn(Optional.of(transaction));

        assertThrows(NoSuchElementException.class,
                () -> transactionService.getTransaction(1L, 10L));
    }
}

