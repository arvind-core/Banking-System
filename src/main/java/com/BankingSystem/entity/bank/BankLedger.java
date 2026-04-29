package com.BankingSystem.entity.bank;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_ledger")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal totalCapital;

    @Column(nullable = false)
    private BigDecimal totalDeposits;

    @Column(nullable = false)
    private BigDecimal totalLoanBook;

    @Column(nullable = false)
    private BigDecimal totalCreditExposure;

    @Column(nullable = false)
    private BigDecimal totalReserve;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate(){
        lastUpdated = LocalDateTime.now();
    }

    public BigDecimal getAvailableLendingCapacity(){
        BigDecimal totalFunds = totalCapital.add(totalDeposits);
        BigDecimal maxLendable = totalFunds.multiply(BigDecimal.valueOf(0.80));
        BigDecimal currentExposure = totalLoanBook.add(totalCreditExposure);

        return maxLendable.subtract(currentExposure).max(BigDecimal.ZERO);
    }

    public boolean canLend(BigDecimal amount){
        return getAvailableLendingCapacity().compareTo(amount) >= 0;
    }









































}
