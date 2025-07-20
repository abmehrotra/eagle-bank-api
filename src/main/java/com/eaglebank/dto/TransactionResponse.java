package com.eaglebank.dto;

import com.eaglebank.model.TransactionType;

public record TransactionResponse(
        Long transactionId,
        TransactionType type,
        Double amount,
        Double updatedBalance
) {
}
