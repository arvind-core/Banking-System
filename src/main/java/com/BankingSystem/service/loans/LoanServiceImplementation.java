package com.BankingSystem.service.loans;

import com.BankingSystem.BankConfig;
import com.BankingSystem.dto.request.LoanApplicationRequest;
import com.BankingSystem.dto.request.LoanReviewRequest;
import com.BankingSystem.dto.response.EMICalculationPreviewResponse;
import com.BankingSystem.dto.response.EMIScheduleResponse;
import com.BankingSystem.dto.response.LoanAccountResponse;
import com.BankingSystem.dto.response.LoanApplicationResponse;
import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.bank.Branch;
import com.BankingSystem.entity.loan.*;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.InvalidOperationException;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.*;
import com.BankingSystem.service.bank.BankLedgerService;
import com.BankingSystem.service.trust.TrustScoreService;
import com.BankingSystem.util.EMICalculator;
import com.BankingSystem.util.NotificationEvent;
import com.BankingSystem.util.NotificationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImplementation implements LoanService{

    private final AccountRepository accountRepository;
    private final TrustScoreService trustScoreService;
    private final LoanApplicationRepository loanApplicationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final EMIScheduleRepository emiScheduleRepository;
    private final BranchRepository branchRepository;
    private final BankLedgerService bankLedgerService;

    @Override
    @Transactional
    public LoanApplicationResponse applyForLoan(LoanApplicationRequest request) {

        Account account = accountRepository
                .findByAccountNumberAndIsActiveTrue(request
                        .getAccountNumber()).orElseThrow(() -> new ResourceNotFoundException(
                                "Account not found : " + request.getAccountNumber()));

        User user = account.getUser();

        if(user.getDateOfBirth() == null){
            throw new InvalidOperationException("Date of Birth is required for Loan Application");
        }

        int age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();

        if(age < BankConfig.MIN_LOAN_AGE){
            throw new InvalidOperationException("Minimum Loan age is : " + BankConfig.MIN_LOAN_AGE + "years");
        }

        if(age > BankConfig.MAX_LOAN_AGE && request.getLoanType() != LoanType.HOME){
            throw new InvalidOperationException("Maximum age for this loan is : " + BankConfig.MAX_LOAN_AGE + "years");
        }

        if(!trustScoreService.isEligibleForLoan(user.getId())){
            throw new InvalidOperationException(
                    "Your Trust Score is too low to apply for a loan. "
                    + "Please Improve Your Banking history and try again."
            );
        }

        long activeApplications = loanApplicationRepository.countByUserAndStatusIn(user,
                List.of(LoanStatus.PENDING,
                        LoanStatus.UNDER_REVIEW,
                        LoanStatus.APPROVED));

        if(activeApplications >= BankConfig.MAX_LOAN_APPLICATIONS){
            trustScoreService.decreaseScore(user.getId(),
                    Math.abs(BankConfig.SCORE_MULTIPLE_LOAN_APPLICATIONS),
                    "Multiple Simultaneous Loan Applications");
            throw new InvalidOperationException("You already have " + activeApplications + "active loan applications. Maximum "
                    + BankConfig.MAX_LOAN_APPLICATIONS + "allowed simultaneously.");
        }

        // Annual Income Check

        if(user.getAnnualIncome() == null || user.getAnnualIncome().compareTo(BigDecimal.ZERO) <= 0){
            throw new InvalidOperationException("Annual Income must be declared before applying for a loan.");
        }

        // Basic affordability check - EMI should not exceed 50% of monthly income

        BigDecimal interestRate = getInterestRateForLoanType(request.getLoanType(),user.getTrustScore());

        BigDecimal emi = EMICalculator.
                calculateEMI(request.getRequestedAmount(),
                                interestRate,
                                request.getTenureMonths(),
                                request.getInterestMethod());

        BigDecimal monthlyIncome = user.getAnnualIncome().divide(BigDecimal.valueOf(12),2, RoundingMode.HALF_UP);

        if(emi.compareTo(monthlyIncome.multiply(BigDecimal.valueOf(0.5))) > 0){
            throw new InvalidOperationException(
                    "EMI of ₹" + emi + " exceeds 50% of your monthly income ₹"
                            + monthlyIncome + ". Please reduce loan amount or extend tenure.");
        }

        LoanApplication application = LoanApplication.builder()
                .applicationReference(generateApplicationReference())
                .user(user)
                .account(account)
                .branch(account.getBranch())
                .loanType(request.getLoanType())
                .interestMethod(request.getInterestMethod())
                .requestedAmount(request.getRequestedAmount())
                .tenureMonths(request.getTenureMonths())
                .purpose(request.getPurpose())
                .status(LoanStatus.PENDING)
                .build();

        LoanApplication saved = loanApplicationRepository.save(application);

        // notify customer

        Map<String, Object> data = new HashMap<>();
        data.put("applicationReference", saved.getApplicationReference());
        data.put("loanType", saved.getLoanType().toString());
        data.put("requestedAmount", saved.getRequestedAmount());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.LOAN_APPLICATION_SUBMITTED,
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                data));

        return mapToApplicationResponse(saved);
    }

    @Override
    @Transactional
    public LoanApplicationResponse reviewLoanApplication(LoanReviewRequest request) {

        LoanApplication application = loanApplicationRepository
                .findByApplicationReference(request
                        .getApplicationReference()).orElseThrow(() -> new ResourceNotFoundException(
                                "Loan Application not found : " + request.getApplicationReference()));

        if(application.getStatus() != LoanStatus.PENDING && application.getStatus() != LoanStatus.UNDER_REVIEW){
            throw new InvalidOperationException("Application is already " + application.getStatus() + " and can't be reviewed again.");
        }

        if(request.getDecision() != LoanStatus.APPROVED && request.getDecision() != LoanStatus.REJECTED && request.getDecision() != LoanStatus.UNDER_REVIEW){
            throw new InvalidOperationException("Invalid decision. Must be APPROVED, REJECTED, or UNDER_REVIEW.");
        }

        User reviewer = userRepository.findById(request.getReviewByUserId()).orElseThrow(() -> new ResourceNotFoundException("Reviewer not found " + request.getReviewByUserId()));

        application.setStatus(request.getDecision());
        application.setManagerComments(request.getComments());
        application.setReviewedBy(reviewer);
        application.setReviewedAt(LocalDateTime.now());

        LoanApplication updated = loanApplicationRepository.save(application);

        User applicant = application.getUser();

        Map<String, Object> data = new HashMap<>();
        data.put("applicationReference", application.getApplicationReference());
        data.put("loanType", application.getLoanType().toString());
        data.put("requestedAmount", application.getRequestedAmount());
        data.put("comments", request.getComments());

        NotificationEventType eventType = request.getDecision() == LoanStatus.APPROVED
                ? NotificationEventType.LOAN_APPROVED
                : request.getDecision() == LoanStatus.REJECTED
                  ? NotificationEventType.LOAN_REJECTED
                  : NotificationEventType.LOAN_UNDER_REVIEW;

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                eventType,
                applicant.getFirstName() + " " + applicant.getLastName(),
                applicant.getEmail(),
                applicant.getPhoneNumber(),
                data
                ));

        // Update trust score on rejection
        if (request.getDecision() == LoanStatus.REJECTED) {
            trustScoreService.decreaseScore(applicant.getId(), 1,
                    "Loan application rejected");
        }

        return mapToApplicationResponse(updated);
    }

    @Override
    @Transactional
    public LoanAccountResponse disburseLoan(String applicationReference) {

        LoanApplication application = loanApplicationRepository
                .findByApplicationReference(applicationReference)
                .orElseThrow( () -> new ResourceNotFoundException("Loan application not found : " + applicationReference));

        if (!bankLedgerService.canLend(application.getRequestedAmount())) {
            throw new InvalidOperationException(
                    "Bank does not have sufficient lending capacity " +
                            "to process this loan at this time.");
        }

        if(application.getStatus() != LoanStatus.APPROVED){
            throw new InvalidOperationException("Only Approved applications can be disbursed");
        }

        User user = application.getUser();
        Account disbursementAccount = application.getAccount();
        Branch branch = application.getBranch();

        BigDecimal interestRate = getInterestRateForLoanType(application.getLoanType(), user.getTrustScore());

        BigDecimal emi = EMICalculator.calculateEMI(
                application.getRequestedAmount(),
                interestRate,
                application.getTenureMonths(),
                application.getInterestMethod()
        );

        BigDecimal totalInterest = EMICalculator.calculateTotalInterest(
                application.getRequestedAmount(),
                interestRate,
                application.getTenureMonths(),
                application.getInterestMethod()
        );

        BigDecimal totalPayable = application.getRequestedAmount().add(totalInterest);

        disbursementAccount.setBalance(disbursementAccount.getBalance().add(application.getRequestedAmount()));

        accountRepository.save(disbursementAccount);

        bankLedgerService.onLoanDisbursed(application.getRequestedAmount());

        application.setStatus(LoanStatus.DISBURSED);
        loanApplicationRepository.save(application);

        LocalDate disbursementDate = LocalDate.now();

        LoanAccount loanAccount = LoanAccount.builder()
                .loanAccountNumber(generateLoanAccountNumber())
                .application(application)
                .user(user)
                .disbursementAccount(disbursementAccount)
                .originatingBranch(branch)
                .currentBranch(branch)
                .loanType(application.getLoanType())
                .interestMethod(application.getInterestMethod())
                .principalAmount(application.getRequestedAmount())
                .annualInterestRate(interestRate)
                .tenureMonths(application.getTenureMonths())
                .emiAmount(emi)
                .outstandingPrincipal(application.getRequestedAmount())
                .totalInterestPayable(totalInterest)
                .totalAmountPayable(totalPayable)
                .totalAmountPaid(BigDecimal.ZERO)
                .emisPaid(0)
                .emisRemaining(application.getTenureMonths())
                .nextEmiDate(disbursementDate.plusMonths(1))
                .disbursementDate(disbursementDate)
                .status(LoanStatus.ACTIVE)
                .build();

        LoanAccount savedLoanAccount = loanAccountRepository.save(loanAccount);

        // Generate EMI schedule

        List<EMIScheduleResponse> scheduleResponses = EMICalculator.generateAmortizationSchedule(
                application.getRequestedAmount(),
                interestRate,
                application.getTenureMonths(),
                application.getInterestMethod(),
                disbursementDate
        );

        scheduleResponses.forEach( s -> {
            EMISchedule emiSchedule = EMISchedule.builder()
                    .loanAccount(savedLoanAccount)
                    .installmentNumber(s.getInstallmentNumber())
                    .dueDate(s.getDueDate())
                    .emiAmount(s.getEmiAmount())
                    .principalComponent(s.getPrincipalComponent())
                    .interestComponent(s.getInterestComponent())
                    .outstandingPrincipalAfter(s.getOutstandingPrincipalAfter())
                    .status(EMIStatus.PENDING)
                    .build();

            emiScheduleRepository.save(emiSchedule);
        });

        Map<String, Object> data = new HashMap<>();
        data.put("loanAccountNumber",savedLoanAccount.getLoanAccountNumber());
        data.put("loanAmount",application.getRequestedAmount());
        data.put("emiAmount",emi);
        data.put("tenure",application.getTenureMonths());
        data.put("interestRate",interestRate);

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.LOAN_DISBURSED,
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                data));

        return mapToLoanAccountResponse(savedLoanAccount);
    }


    @Override
    public List<LoanApplicationResponse> getUserLoanApplications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId));
        return loanApplicationRepository.findByUser(user)
                .stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LoanApplicationResponse> getPendingApplicationsForBranch(
            Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch not found: " + branchId));
        return loanApplicationRepository
                .findByBranchAndStatus(branch, LoanStatus.PENDING)
                .stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LoanAccountResponse getLoanAccount(String loanAccountNumber) {
        LoanAccount loanAccount = loanAccountRepository
                .findByLoanAccountNumber(loanAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan account not found: " + loanAccountNumber));
        return mapToLoanAccountResponse(loanAccount);
    }

    @Override
    public List<LoanAccountResponse> getUserActiveLoans(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId));
        return loanAccountRepository.findByUserAndStatus(user, LoanStatus.ACTIVE)
                .stream()
                .map(this::mapToLoanAccountResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<EMIScheduleResponse> getLoanEMISchedule(
            String loanAccountNumber) {
        LoanAccount loanAccount = loanAccountRepository
                .findByLoanAccountNumber(loanAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan account not found: " + loanAccountNumber));
        return emiScheduleRepository
                .findByLoanAccountOrderByInstallmentNumberAsc(loanAccount)
                .stream()
                .map(this::mapToEMIScheduleResponse)
                .collect(Collectors.toList());
    }

    @Override
    public EMICalculationPreviewResponse previewEMI(BigDecimal amount,
                                                    BigDecimal annualRate,
                                                    Integer tenureMonths,
                                                    String interestMethod) {
        InterestMethod method = InterestMethod.valueOf(interestMethod.toUpperCase());

        BigDecimal emi = EMICalculator.calculateEMI(
                amount, annualRate, tenureMonths, method);
        BigDecimal totalInterest = EMICalculator.calculateTotalInterest(
                amount, annualRate, tenureMonths, method);
        List<EMIScheduleResponse> schedule =
                EMICalculator.generateAmortizationSchedule(
                        amount, annualRate, tenureMonths, method,
                        LocalDate.now());

        return EMICalculationPreviewResponse.builder()
                .principalAmount(amount)
                .annualInterestRate(annualRate)
                .tenureMonths(tenureMonths)
                .interestMethod(method)
                .monthlyEMI(emi)
                .totalInterestPayable(totalInterest)
                .totalAmountPayable(amount.add(totalInterest))
                .amortizationSchedule(schedule)
                .build();
    }

    // Interest rate based on loan type and trust score
    private BigDecimal getInterestRateForLoanType(LoanType loanType,
                                                  int trustScore) {
        double baseRate = switch (loanType) {
            case HOME -> BankConfig.LOAN_INTEREST_RATE_FOR_HOME;
            case CAR -> BankConfig.LOAN_INTEREST_RATE_FOR_CAR;
            case EDUCATION -> BankConfig.LOAN_INTEREST_RATE_FOR_EDUCATION;
            case PERSONAL -> BankConfig.LOAN_INTEREST_RATE_FOR_PERSONAL;
            case BUSINESS -> BankConfig.LOAN_INTEREST_RATE_FOR_BUSINESS;
            case GOLD -> BankConfig.LOAN_INTEREST_RATE_FOR_GOLD;
        };

        // Better trust score gets lower rate
        double discount = 0.0;
        if (trustScore >= BankConfig.TRUST_SCORE_PREMIUM) {
            discount = 1.0;
        } else if (trustScore >= BankConfig.TRUST_SCORE_GOOD) {
            discount = 0.5;
        } else if (trustScore < BankConfig.TRUST_SCORE_RISKY) {
            discount = -1.0; // Higher rate for risky customers
        }

        return BigDecimal.valueOf(baseRate - discount);
    }

    private String generateApplicationReference() {
        return "LOAN-" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();
    }

    private String generateLoanAccountNumber() {
        return "LA" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }

    private LoanApplicationResponse mapToApplicationResponse(
            LoanApplication application) {
        return LoanApplicationResponse.builder()
                .id(application.getId())
                .applicationReference(application.getApplicationReference())
                .applicantName(application.getUser().getFirstName()
                        + " " + application.getUser().getLastName())
                .accountNumber(application.getAccount().getAccountNumber())
                .branchName(application.getBranch().getBranchName())
                .loanType(application.getLoanType())
                .interestMethod(application.getInterestMethod())
                .requestedAmount(application.getRequestedAmount())
                .tenureMonths(application.getTenureMonths())
                .purpose(application.getPurpose())
                .status(application.getStatus())
                .managerComments(application.getManagerComments())
                .createdAt(application.getCreatedAt())
                .reviewedAt(application.getReviewedAt())
                .build();
    }

    private LoanAccountResponse mapToLoanAccountResponse(
            LoanAccount loanAccount) {
        return LoanAccountResponse.builder()
                .id(loanAccount.getId())
                .loanAccountNumber(loanAccount.getLoanAccountNumber())
                .borrowerName(loanAccount.getUser().getFirstName()
                        + " " + loanAccount.getUser().getLastName())
                .loanType(loanAccount.getLoanType())
                .interestMethod(loanAccount.getInterestMethod())
                .principalAmount(loanAccount.getPrincipalAmount())
                .annualInterestRate(loanAccount.getAnnualInterestRate())
                .tenureMonths(loanAccount.getTenureMonths())
                .emiAmount(loanAccount.getEmiAmount())
                .outstandingPrincipal(loanAccount.getOutstandingPrincipal())
                .totalInterestPayable(loanAccount.getTotalInterestPayable())
                .totalAmountPayable(loanAccount.getTotalAmountPayable())
                .totalAmountPaid(loanAccount.getTotalAmountPaid())
                .emisPaid(loanAccount.getEmisPaid())
                .emisRemaining(loanAccount.getEmisRemaining())
                .nextEmiDate(loanAccount.getNextEmiDate())
                .disbursementDate(loanAccount.getDisbursementDate())
                .status(loanAccount.getStatus())
                .originatingBranch(loanAccount.getOriginatingBranch()
                        .getBranchName())
                .currentBranch(loanAccount.getCurrentBranch().getBranchName())
                .createdAt(loanAccount.getCreatedAt())
                .build();
    }

    private EMIScheduleResponse mapToEMIScheduleResponse(EMISchedule emi) {
        return EMIScheduleResponse.builder()
                .id(emi.getId())
                .installmentNumber(emi.getInstallmentNumber())
                .dueDate(emi.getDueDate())
                .emiAmount(emi.getEmiAmount())
                .principalComponent(emi.getPrincipalComponent())
                .interestComponent(emi.getInterestComponent())
                .outstandingPrincipalAfter(emi.getOutstandingPrincipalAfter())
                .status(emi.getStatus())
                .paidDate(emi.getPaidDate())
                .penaltyAmount(emi.getPenaltyAmount())
                .build();
    }
}