package com.eaglebank.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String accountType;

    @NotNull
    private Double balance;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public BankAccount() {
    }

    public BankAccount(Long id, String accountType, Double balance, User user) {
        this.id = id;
        this.accountType = accountType;
        this.balance = balance;
        this.user = user;
    }

}
