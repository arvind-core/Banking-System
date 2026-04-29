package com.BankingSystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BankLedgerResponse {
    private BigDecimal totalCapital;
    private BigDecimal totalDeposits;
    private BigDecimal totalLoanBook;
    private BigDecimal totalCreditExposure;
    private BigDecimal availableLendingCapacity;
    private BigDecimal totalReserve;
    private double lendingUtilizationPercentage;
    private LocalDateTime lastUpdated;
}
