package com.BankingSystem.dto.response.managerDashoboard;

import com.BankingSystem.dto.response.LoanApplicationResponse;
import com.BankingSystem.dto.response.branchTransfer.BranchTransferResponse;
import com.BankingSystem.dto.response.creditcard.CreditCardResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ManagerDashboardResponse {

    private String branchName;
    private String branchCode;

    // Pending counts - quick overview

    private int pendingLoanApplications;
    private int pendingCardApplications;
    private int pendingBranchTransfersCurrent;
    private int pendingBranchTransfersNew;

    // Branch Metrics
    private BigDecimal totalDepositsInBranch;
    private BigDecimal totalActiveLoanBook;
    private int totalActiveAccounts;
    private int totalActiveLoans;
    private int totalActiveCards;

    // Pending Items
    private List<LoanApplicationResponse> pendingLoans;
    private List<CreditCardResponse> pendingCreditCards;
    private List<BranchTransferResponse> pendingTransfersCurrent;
    private List<BranchTransferResponse> pendingTransfersNew;
}
