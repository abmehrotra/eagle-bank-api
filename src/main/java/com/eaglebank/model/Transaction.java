package com.eaglebank.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private BankAccount bankAccount;

    @Column(name = "balance_after_transaction")
    private Double balanceAfterTransaction;

    private Transaction() {
    }

    public Transaction(Double amount, TransactionType type, LocalDateTime timestamp, BankAccount bankAccount) {
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
        this.bankAccount = bankAccount;
    }

}
