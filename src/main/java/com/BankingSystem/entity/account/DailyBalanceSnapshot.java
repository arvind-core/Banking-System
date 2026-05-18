package com.BankingSystem.entity.account;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_balance_snapshots",
    indexes = {
        @Index(name = "idx_snapshot_account_date",
               columnList = "account_id, snapshotDate",
               unique = true),
        @Index(name = "idx_snapshot_date", columnList = "snapshotDate")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyBalanceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false)
    private BigDecimal closingBalance;

    @Column(nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
}