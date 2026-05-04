package com.BankingSystem.entity.card;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_transactions",
        indexes = {
                @Index(name = "idx_card_tx_card_id", columnList = "credit_card_id"),
                @Index(name = "idx_card_tx_created_at", columnList = "createdAt")
        })
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CardTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = false)
    private String transactionReference;

    @ManyToOne
    @JoinColumn(name = "credit_card_id", nullable = false)
    private CreditCard creditCard;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String merchantName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpendCategory category;

    @Column(nullable = false)
    private BigDecimal rewardPointsEarned;

    @Column(nullable = false)
    private BigDecimal availableLimitAfter;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}