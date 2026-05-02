package com.BankingSystem.Controller;

import com.BankingSystem.dto.request.LoanApplicationRequest;
import com.BankingSystem.dto.request.LoanReviewRequest;
import com.BankingSystem.dto.response.emis.EMICalculationPreviewResponse;
import com.BankingSystem.dto.response.emis.EMIScheduleResponse;
import com.BankingSystem.dto.response.LoanAccountResponse;
import com.BankingSystem.dto.response.LoanApplicationResponse;
import com.BankingSystem.service.loans.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;

    @PostMapping("/apply")
    public ResponseEntity<LoanApplicationResponse> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanService.applyForLoan(request));
    }

    @PatchMapping("/review")
    public ResponseEntity<LoanApplicationResponse> reviewApplication(@Valid @RequestBody LoanReviewRequest request){
        return ResponseEntity.ok(loanService.reviewLoanApplication(request));
    }

    @PostMapping("/disburse/{applicationReference}")
    public ResponseEntity<LoanAccountResponse> disburseLoan(@PathVariable String applicationReference){
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.disburseLoan(applicationReference));
    }

    @GetMapping("/applications/user/{userId}")
    public ResponseEntity<List<LoanApplicationResponse>> getUserApplications(@PathVariable Long userId){
        return ResponseEntity.ok(loanService.getUserLoanApplications(userId));
    }

    @GetMapping("applications/branch/{branchId}/pending")
    public ResponseEntity<List<LoanApplicationResponse>> getPendingApplications(@PathVariable Long branchId){
        return ResponseEntity.ok(loanService.getPendingApplicationsForBranch(branchId));
    }

    @GetMapping("/account/{loanAccountNumber}")
    public ResponseEntity<LoanAccountResponse> getLoanAccount(@PathVariable String loanAccountNumber){
        return ResponseEntity.ok(loanService.getLoanAccount(loanAccountNumber));
    }

    @GetMapping("/active/user/{userId}")
    public ResponseEntity<List<LoanAccountResponse>> getUserActiveLoans(
            @PathVariable Long userId) {
        return ResponseEntity.ok(loanService.getUserActiveLoans(userId));
    }

    @GetMapping("/emi-schedule/{loanAccountNumber}")
    public ResponseEntity<List<EMIScheduleResponse>> getEMISchedule(@PathVariable String loanAccountNumber){
        return ResponseEntity.ok(loanService.getLoanEMISchedule(loanAccountNumber));
    }

    @GetMapping("/preview-emi")
    public ResponseEntity<EMICalculationPreviewResponse> previewEMI(
            @RequestParam BigDecimal amount,
            @RequestParam BigDecimal annualRate,
            @RequestParam Integer tenureMonths,
            @RequestParam String interestMethod){
        return ResponseEntity.ok(loanService.previewEMI(amount,annualRate,tenureMonths,interestMethod));
    }
}


















