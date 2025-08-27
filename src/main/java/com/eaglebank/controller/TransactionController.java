package com.eaglebank.controller;

import com.eaglebank.dto.TransactionRequest;
import com.eaglebank.dto.TransactionResponse;
import com.eaglebank.service.TransactionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
        log.info("Creating transaction for accountId={} type={} amount={}",
                accountId, request.type(), request.amount());

        TransactionResponse response = transactionService.createTransaction(accountId, request);

        log.info("Transaction created successfully. accountId={} transactionId={} type={} amount={}",
                accountId, response.transactionId(), response.type(), response.amount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable Long accountId,
            @PathVariable Long transactionId
    ) {
        log.info("Fetching transaction. accountId={} transactionId={}", accountId, transactionId);

        TransactionResponse response = transactionService.getTransaction(accountId, transactionId);

        log.info("Retrieved transaction. accountId={} transactionId={} type={} amount={}",
                accountId, response.transactionId(), response.type(), response.amount());

        return ResponseEntity.ok(response);
    }
}
