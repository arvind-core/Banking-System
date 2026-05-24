package com.BankingSystem.dto.response.admin;

import com.BankingSystem.dto.response.BankLedgerResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SystemDashboardResponse {

    private int totalBranches;
    private int activeBranches;
    private int totalUsers;
    private int totalManagers;
    private int totalCustomers;
    private long totalAccounts;
    private long totalActiveLoans;
    private long totalCreditCards;
    private long totalDebitCards;
    private BigDecimal totalSystemDeposits;
    private BigDecimal totalLoanBook;
    private BigDecimal totalCreditExposure;
    private BigDecimal availableLendingCapacity;
    private BankLedgerResponse bankLedger;
}
