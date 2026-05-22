package com.BankingSystem.entity.card;

import com.BankingSystem.entity.transactions.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "debit_card_transactions",
    indexes = {
            @Index(name = "idx_dc_tx_created_at", columnList = "createdAt"),
            @Index(name = "idx_dc_tx_card_id", columnList = "debit_card_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitCardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = false)
    private String transactionReference;

    @ManyToOne
    @JoinColumn(name = "debit_card_id", nullable = false)
    private DebitCard debitCard;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal balanceAfterTransaction;

    @Column(nullable = false)
    private String merchantName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebitTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebitCardTransactionStatus status;

    @Column
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
    }
}