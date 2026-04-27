package com.BankingSystem.dto.response;

import com.BankingSystem.entity.loan.InterestMethod;
import com.BankingSystem.entity.loan.LoanStatus;
import com.BankingSystem.entity.loan.LoanType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoanApplicationResponse {
    private Long id;
    private String applicationReference;
    private String applicantName;
    private String accountNumber;
    private String branchName;
    private LoanType loanType;
    private InterestMethod interestMethod;
    private BigDecimal requestedAmount;
    private Integer tenureMonths;
    private String purpose;
    private LoanStatus status;
    private String managerComments;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
