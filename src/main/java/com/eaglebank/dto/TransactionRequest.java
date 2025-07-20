package com.eaglebank.dto;


import com.eaglebank.model.TransactionType;
import jakarta.validation.constraints.NotNull;

public record TransactionRequest(
        @NotNull(message = "Amount is required") Double amount,
        @NotNull(message = "Transaction type is required") TransactionType type
) {
}
