package com.BankingSystem.dto.request;

import com.BankingSystem.BankConfig;
import com.BankingSystem.entity.loan.InterestMethod;
import com.BankingSystem.entity.loan.LoanType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "loan type is required")
    private LoanType loanType;

    @NotNull(message = "interest method is required")
    private InterestMethod interestMethod;

    @NotNull(message = "requested amount is required")
    @DecimalMin(value = "10000.0", message = "Minimum loan amount is ₹10,000")
    private BigDecimal requestedAmount;

    @NotNull(message = "tenure is required")
    @Min(value = BankConfig.MIN_TENURE_MONTHS_FOR_LOAN,message = "minimum tenure is" + BankConfig.MIN_TENURE_MONTHS_FOR_LOAN + " months")
    @Max(value = BankConfig.MAX_TENURE_MONTHS_FOR_LOAN, message = "maximum tenure is" + BankConfig.MAX_TENURE_MONTHS_FOR_LOAN + "months")
    private Integer tenureMonths;

    @NotNull(message = "purpose is required")
    private String purpose;



}
