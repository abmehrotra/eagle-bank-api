package com.eaglebank.service;

import com.eaglebank.dto.BankAccountRequest;
import com.eaglebank.dto.BankAccountResponse;
import com.eaglebank.model.BankAccount;
import com.eaglebank.model.User;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class BankAccountService {

    private final BankAccountRepository accountRepository;
    private final UserRepository userRepository;

    public BankAccountService(BankAccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public BankAccountResponse createAccount(BankAccountRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BankAccount account = new BankAccount();
        account.setAccountType(request.accountType());
        account.setBalance(request.balance());
        account.setUser(user);

        BankAccount saved = accountRepository.save(account);

        return new BankAccountResponse(saved.getId(), saved.getAccountType(), saved.getBalance());
    }

    public BankAccountResponse getAccountById(Long accountId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Bank account not found"));

        if (!account.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied to this account");
        }

        return new BankAccountResponse(account.getId(), account.getAccountType(), account.getBalance());
    }
}
