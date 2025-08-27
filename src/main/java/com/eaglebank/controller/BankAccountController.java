package com.eaglebank.controller;

import com.eaglebank.dto.BankAccountRequest;
import com.eaglebank.dto.BankAccountResponse;
import com.eaglebank.service.BankAccountService;
import com.eaglebank.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/accounts")
public class BankAccountController {

    private final BankAccountService service;

    @Autowired
    private SecurityUtils securityUtils;

    public BankAccountController(BankAccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BankAccountResponse> createAccount(@Valid @RequestBody BankAccountRequest request) {
        log.info("Received request to create bank account for userId={}", securityUtils.getAuthenticatedUser().getId());

        BankAccountResponse response = service.createAccount(request);

        log.info("Successfully created bank account. accountId={} userId={}",
                response.id(), securityUtils.getAuthenticatedUser().getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<BankAccountResponse> getAccount(@PathVariable Long accountId) {
        log.info("Fetching bank account with accountId={}", accountId);

        BankAccountResponse response = service.getAccountById(accountId);

        log.info("Retrieved bank account. accountId={} userId={}",
                response.id(), securityUtils.getAuthenticatedUser().getId());

        return ResponseEntity.ok(response);
    }
}
