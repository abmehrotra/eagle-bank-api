package com.eaglebank.controller;

import com.eaglebank.dto.TransactionRequest;
import com.eaglebank.dto.TransactionResponse;
import com.eaglebank.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/accounts/{accountId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @PathVariable Long accountId,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(accountId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable Long accountId,
            @PathVariable Long transactionId
    ) {
        TransactionResponse response = transactionService.getTransaction(accountId, transactionId);
        return ResponseEntity.ok(response);
    }
}
