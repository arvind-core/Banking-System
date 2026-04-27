package com.BankingSystem.service.loans;

import com.BankingSystem.dto.request.LoanApplicationRequest;
import com.BankingSystem.dto.request.LoanReviewRequest;
import com.BankingSystem.dto.response.EMICalculationPreviewResponse;
import com.BankingSystem.dto.response.EMIScheduleResponse;
import com.BankingSystem.dto.response.LoanAccountResponse;
import com.BankingSystem.dto.response.LoanApplicationResponse;
import java.math.BigDecimal;
import java.util.List;

public interface LoanService {

    LoanApplicationResponse applyForLoan(LoanApplicationRequest request);

    LoanApplicationResponse reviewLoanApplication(LoanReviewRequest request);

    LoanAccountResponse disburseLoan(String applicationReference);

    List<LoanApplicationResponse> getUserLoanApplications(Long userId);

    List<LoanApplicationResponse> getPendingApplicationsForBranch(Long branchId);

    LoanAccountResponse getLoanAccount(String loanAccountNumber);

    List<LoanAccountResponse> getUserActiveLoans(Long userId);

    List<EMIScheduleResponse> getLoanEMISchedule(String loanAccountNumber);

    EMICalculationPreviewResponse previewEMI(BigDecimal amount,
                                             BigDecimal annualRate,
                                             Integer tenureMonths,
                                             String interestMethod);
}