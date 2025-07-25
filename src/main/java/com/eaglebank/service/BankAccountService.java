package com.eaglebank.service;

import com.eaglebank.dto.BankAccountRequest;
import com.eaglebank.dto.BankAccountResponse;
import com.eaglebank.model.BankAccount;
import com.eaglebank.model.User;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.util.AccessValidator;
import com.eaglebank.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class BankAccountService {

    private final BankAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public BankAccountService(BankAccountRepository accountRepository,
                              UserRepository userRepository,
                              SecurityUtils securityUtils) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    public BankAccountResponse createAccount(BankAccountRequest request) {
        User user = securityUtils.getAuthenticatedUser();

        BankAccount account = new BankAccount();
        account.setAccountType(request.accountType());
        account.setBalance(request.balance());
        account.setUser(user);

        BankAccount saved = accountRepository.save(account);

        return new BankAccountResponse(saved.getId(), saved.getAccountType(), saved.getBalance());
    }

    public BankAccountResponse getAccountById(Long accountId) {
        User currentUser = securityUtils.getAuthenticatedUser();

        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Bank account not found"));

        AccessValidator.validateOwnership(account.getUser(), currentUser);

        return new BankAccountResponse(account.getId(), account.getAccountType(), account.getBalance());
    }

    public List<BankAccountResponse> getAccountsForCurrentUser() {
        User user = securityUtils.getAuthenticatedUser();

        return accountRepository.findAllByUserId(user.getId()).stream()
                .map(account -> new BankAccountResponse(
                        account.getId(),
                        account.getAccountType(),
                        account.getBalance()))
                .toList();
    }

    public BankAccountResponse updateAccount(Long accountId, BankAccountRequest request) {
        User currentUser = securityUtils.getAuthenticatedUser();

        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Bank account not found"));

        AccessValidator.validateOwnership(account.getUser(), currentUser);

        account.setAccountType(request.accountType());
        account.setBalance(request.balance());

        BankAccount updated = accountRepository.save(account);

        return new BankAccountResponse(updated.getId(), updated.getAccountType(), updated.getBalance());
    }

    public void deleteAccountById(Long accountId) {
        User currentUser = securityUtils.getAuthenticatedUser();

        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Bank account not found"));

        AccessValidator.validateOwnership(account.getUser(), currentUser);

        accountRepository.delete(account);
    }
}
