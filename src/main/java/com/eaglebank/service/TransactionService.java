package com.eaglebank.service;

import com.eaglebank.dto.TransactionRequest;
import com.eaglebank.dto.TransactionResponse;
import com.eaglebank.exception.InsufficientFundsException;
import com.eaglebank.model.BankAccount;
import com.eaglebank.model.Transaction;
import com.eaglebank.model.TransactionType;
import com.eaglebank.model.User;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.TransactionRepository;
import com.eaglebank.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository accountRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              BankAccountRepository accountRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public TransactionResponse createTransaction(Long accountId, TransactionRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Bank account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied to this account");
        }

        double updatedBalance = account.getBalance();
        if (request.type() == TransactionType.DEPOSIT) {
            updatedBalance += request.amount();
        } else if (request.type() == TransactionType.WITHDRAWAL) {
            if (request.amount() > account.getBalance()) {
                throw new InsufficientFundsException("Insufficient funds");
            }
            updatedBalance -= request.amount();
        }

        account.setBalance(updatedBalance);
        accountRepository.save(account);

        Transaction transaction = new Transaction(request.amount(), request.type(), LocalDateTime.now(), account);
        Transaction saved = transactionRepository.save(transaction);

        return new TransactionResponse(saved.getId(), saved.getType(), saved.getAmount(), updatedBalance);
    }

    public TransactionResponse getTransaction(Long accountId, Long transactionId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Bank account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied to this account");
        }

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found"));

        if (!transaction.getBankAccount().getId().equals(accountId)) {
            throw new NoSuchElementException("Transaction does not belong to this account");
        }

        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                account.getBalance()
        );
    }
}
