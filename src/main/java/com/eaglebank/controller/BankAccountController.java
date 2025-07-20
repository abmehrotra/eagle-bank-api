package com.eaglebank.controller;

import com.eaglebank.dto.BankAccountRequest;
import com.eaglebank.dto.BankAccountResponse;
import com.eaglebank.service.BankAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/accounts")
public class BankAccountController {

    private final BankAccountService service;

    public BankAccountController(BankAccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BankAccountResponse> createAccount(@Valid @RequestBody BankAccountRequest request) {
        BankAccountResponse response = service.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<BankAccountResponse> getAccount(@PathVariable Long accountId) {
        BankAccountResponse response = service.getAccountById(accountId);
        return ResponseEntity.ok(response);
    }
}
