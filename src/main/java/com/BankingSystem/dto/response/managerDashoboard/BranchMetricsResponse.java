package com.BankingSystem.dto.response.managerDashoboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BranchMetricsResponse {

    private String branchName;
    private String branchCode;
    private String city;
    private int totalAccounts;
    private int totalActiveAccounts;
    private int totalClosedAccounts;
    private BigDecimal totalDeposits;
    private int totalLoans;
    private int activeLoans;
    private BigDecimal totalLoanBook;
    private BigDecimal totalCreditExposure;
    private int totalCreditCards;
    private int activeCreditCards;
}
