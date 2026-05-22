package com.BankingSystem.entity.card;

import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.users.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "debit_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String cardNumber;

    @Column(nullable = false)
    private String maskedCardNumber;

    @Column(nullable = false)
    private String cardHolderName;

    @JsonIgnore
    @Column(nullable = false)
    private String cardPin; // Bcrypt hashed

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebitCardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebitCardStatus status;

    // Daily limits
    @Column(nullable = false)
    private BigDecimal dailyAtmLimit;

    @Column(nullable = false)
    private BigDecimal dailyPosLimit;

    @Column(nullable = false)
    private BigDecimal dailyOnlineLimit;

    // Feature toggles
    @Column(nullable = false)
    private boolean internationalTransactionsEnabled = false;

    @Column(nullable = false)
    private boolean onlineTransactionsEnabled = false;

    @Column(nullable = false)
    private boolean contactlessEnabled = false;

    @Column(nullable = false)
    private LocalDate expiryDate;

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
