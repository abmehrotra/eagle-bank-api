package com.eaglebank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BankAccountRequest(
        @NotBlank(message = "Account type is required")
        String accountType,

        @NotNull(message = "Initial balance is required")
        Double balance
) {
}
