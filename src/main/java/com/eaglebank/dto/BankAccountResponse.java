package com.eaglebank.dto;

public record BankAccountResponse(
        Long id,
        String accountType,
        Double balance
) {
}
