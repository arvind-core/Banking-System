package com.BankingSystem.entity.requests;

import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "money_requests",
indexes = {
        @Index(name = "idx_money_req_payer", columnList = "payer_id"),
        @Index(name = "idx_money_req_requester", columnList = "requester_id"),
        @Index(name = "idx_money_req_status", columnList = "status"),
        @Index(name = "idx_money_req_expires", columnList = "expiresAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String requestReference;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @ManyToOne
    @JoinColumn(name = "requester_account_id", nullable = false)
    private Account requesterAccount;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MoneyRequestStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime respondedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }



}


























