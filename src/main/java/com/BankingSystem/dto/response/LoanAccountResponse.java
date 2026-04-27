package com.BankingSystem.dto.response;

import com.BankingSystem.entity.loan.InterestMethod;
import com.BankingSystem.entity.loan.LoanStatus;
import com.BankingSystem.entity.loan.LoanType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanAccountResponse {

    private Long id;
    private String loanAccountNumber;
    private String borrowerName;
    private LoanType loanType;
    private InterestMethod interestMethod;
    private BigDecimal principalAmount;
    private BigDecimal annualInterestRate;
    private Integer tenureMonths;
    private BigDecimal emiAmount;
    private BigDecimal outstandingPrincipal;
    private BigDecimal totalInterestPayable;
    private BigDecimal totalAmountPayable;
    private BigDecimal totalAmountPaid;
    private Integer emisPaid;
    private Integer emisRemaining;
    private LocalDate nextEmiDate;
    private LocalDate disbursementDate;
    private LoanStatus status;
    private String originatingBranch;
    private String currentBranch;
    private LocalDateTime createdAt;
}