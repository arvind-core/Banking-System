package com.BankingSystem.entity.loan;

import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.bank.Branch;
import com.BankingSystem.entity.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = false)
    private Long loanAccountNumber;

    @OneToOne
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication application;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "disbursement_account_id",nullable = false)
    private Account disbursementAccount;

    @ManyToOne
    @JoinColumn(name = "originating_branch_id", nullable = false)
    private Branch originatingBranch;

    @ManyToOne
    @JoinColumn(name = "current_branch_id", nullable = false)
    private Branch currentBranch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanType loanType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterestMethod interestMethod;

    @Column(nullable = false)
    private BigDecimal principalAmount;

    @Column(nullable = false)
    private Integer annualInterestRate;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Column(nullable = false)
    private BigDecimal emiAmount;

    @Column(nullable = false)
    private BigDecimal outStandingPrincipal;

    @Column(nullable = false)
    private BigDecimal totalInterestPayable;

    @Column(nullable = false)
    private BigDecimal totalAmountPayable;

    @Column(nullable = false)
    private BigDecimal totalAmountPaid;

    @Column(nullable = false)
    private Integer emisPaid;

    @Column(nullable = false)
    private Integer emisRemaining;

    @Column(nullable = false)
    private LocalDate nextEmiDate;

    @Column(nullable = false)
    private LocalDate disbursementDate;

    @Column
    private LocalDate closureDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
