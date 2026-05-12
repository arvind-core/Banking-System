package com.BankingSystem.entity.loan;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "emi_schedules",
        indexes = {
                @Index(name = "idx_emi_loan_account", columnList = "loan_account_id"),
                @Index(name = "idx_emi_due_date",columnList = "dueDate"),
                @Index(name = "idx_emi_status",columnList = "status")
                }
        )
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EMISchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "loan_account_id",nullable = false)
    private LoanAccount loanAccount;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private BigDecimal emiAmount;

    @Column(nullable = false)
    private BigDecimal principalComponent;

    @Column(nullable = false)
    private BigDecimal interestComponent;

    @Column(nullable = false)
    private BigDecimal outstandingPrincipalAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EMIStatus status;

    @Column
    private LocalDate paidDate;

    @Column
    private BigDecimal penaltyAmount;

    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column
    private LocalDate lastRetryDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }






















}
