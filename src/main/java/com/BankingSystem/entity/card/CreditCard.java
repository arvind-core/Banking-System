package com.BankingSystem.entity.card;

import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String cardNumber;

    @Column(nullable = false)
    private String maskedCardNumber;

    @Column(nullable = false)
    private String cardHolderName;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "linked_account_id",nullable = false)
    private Account linkedAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;

    @Column(nullable = false)
    private BigDecimal creditLimit;

    @Column(nullable = false)
    private BigDecimal availableLimit;

    @Column(nullable = false)
    private BigDecimal outstandingAmount;

    @Column(nullable = false)
    private BigDecimal minimumDue;

    @Column(nullable = false)
    private BigDecimal totalRewardPoints;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private LocalDate billingCycleStart;

    @Column(nullable = false)
    private LocalDate nextBillingDate;

    @Column(nullable = false)
    private LocalDate paymentDueDate;

    @Column(nullable = false)
    private Integer consecutiveMissedPayments = 0;

    @Column(nullable = false)
    private boolean internationalTransactionsEnabled;

    @Column(nullable = false)
    private boolean onlineTransactionsEnabled;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }
}
