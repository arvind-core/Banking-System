package com.BankingSystem.service.manager;

import com.BankingSystem.dto.response.LoanApplicationResponse;
import com.BankingSystem.dto.response.UserResponse;
import com.BankingSystem.dto.response.branchTransfer.BranchTransferResponse;
import com.BankingSystem.dto.response.creditcard.CreditCardResponse;
import com.BankingSystem.dto.response.managerDashoboard.BranchMetricsResponse;
import com.BankingSystem.dto.response.managerDashoboard.ManagerDashboardResponse;
import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.bank.Branch;
import com.BankingSystem.entity.bank.BranchTransferRequest;
import com.BankingSystem.entity.card.CreditCard;
import com.BankingSystem.entity.loan.LoanApplication;
import com.BankingSystem.entity.loan.LoanStatus;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.*;
import com.BankingSystem.service.trust.TrustScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    private final BranchRepository branchRepository;
    private final AccountRepository accountRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final CreditCardRepository creditCardRepository;
    private final BranchTransferRequestRepository branchTransferRepository;
    private final UserRepository userRepository;
    private final TrustScoreService trustScoreService;


    @Override
    public ManagerDashboardResponse getDashboard(Long branchId) {
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        // Fetch all pending items
        List<LoanApplicationResponse> pendingLoans = loanApplicationRepository
                .findByBranchAndStatus(branch, LoanStatus.PENDING)
                .stream()
                .map(this :: mapToLoanApplicationResponse)
                .collect(Collectors.toList());

        List<CreditCardResponse> pendingCards = creditCardRepository.findPendingApprovalByBranch(branch)
                .stream()
                .map(this :: mapToCardResponse)
                .collect(Collectors.toList());

        List<BranchTransferResponse> pendingCurrent = branchTransferRepository.findPendingForCurrentBranch(branch)
                .stream()
                .map(this :: mapToTransferResponse)
                .collect(Collectors.toList());

        List<BranchTransferResponse> pendingNew = branchTransferRepository.findPendingForNewBranch(branch)
                .stream()
                .map(this :: mapToTransferResponse)
                .collect(Collectors.toList());

        // Branch metrics
        BigDecimal totalDeposits = accountRepository.sumBalancesByBranch(branch);
        BigDecimal totalLoanBook = loanAccountRepository.sumActiveLoanBookByBranch(branch);
        long activeAccounts = accountRepository.findByBranchAndIsActiveTrue(branch).size();
        long activeLoans = loanAccountRepository.countByCurrentBranchAndStatus(branch, LoanStatus.ACTIVE);
        long activeCards = creditCardRepository.countActiveCardsByBranch(branch);

        return ManagerDashboardResponse.builder()
                .branchName(branch.getBranchName())
                .branchCode(branch.getBranchCode())
                .pendingLoanApplications(pendingLoans.size())
                .pendingCardApplications(pendingCards.size())
                .pendingBranchTransfersCurrent(pendingCurrent.size())
                .pendingBranchTransfersNew(pendingNew.size())
                .totalDepositsInBranch(totalDeposits)
                .totalActiveLoanBook(totalLoanBook)
                .totalActiveAccounts((int) activeAccounts)
                .totalActiveLoans((int) activeLoans)
                .totalActiveCards((int) activeCards)
                .pendingLoans(pendingLoans)
                .pendingCreditCards(pendingCards)
                .pendingTransfersCurrent(pendingCurrent)
                .pendingTransfersNew(pendingNew)
                .build();
    }

    @Override
    public BranchMetricsResponse getBranchMetrics(Long branchId) {

        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        List<Account> allAccounts = accountRepository.findByBranch(branch);

        long activeAccounts = allAccounts.stream().filter(Account::isActive).count();
        BigDecimal totalDeposits = accountRepository.sumBalancesByBranch(branch);
        BigDecimal totalLoanBook = loanAccountRepository.sumActiveLoanBookByBranch(branch);
        long activeLoans = loanAccountRepository.countByCurrentBranchAndStatus(branch, LoanStatus.ACTIVE);
        long totalLoans = loanAccountRepository.findByCurrentBranchAndStatus(branch, LoanStatus.ACTIVE).size();
        long activeCards  = creditCardRepository.countActiveCardsByBranch(branch);

        return BranchMetricsResponse.builder()
                .branchName(branch.getBranchName())
                .branchCode(branch.getBranchCode())
                .city(branch.getCity())
                .totalAccounts(allAccounts.size())
                .totalActiveAccounts((int) activeLoans)
                .totalClosedAccounts( (int) (allAccounts.size() - activeAccounts))
                .totalDeposits(totalDeposits)
                .totalLoanBook(totalLoanBook)
                .totalLoans((int) totalLoans)
                .totalCreditCards((int) activeCards)
                .activeCreditCards((int) activeCards)
                .build();
    }

    @Override
    public List<UserResponse> getCustomersInBranch(Long branchId) {

        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        return accountRepository.findByBranchAndIsActiveTrue(branch)
                .stream()
                .map(Account::getUser)
                .distinct()
                .map(this :: mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getCustomerDetails(Long userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        int age = user.getDateOfBirth() != null ? Period.between(user.getDateOfBirth(), LocalDate.now()).getYears() : 0;

        String category = trustScoreService.getScoreCategory(user.getTrustScore());

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .dateOfBirth(user.getDateOfBirth())
                .age(age)
                .annualIncome(user.getAnnualIncome())
                .trustScore(user.getTrustScore())
                .trustScoreCategory(category)
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private LoanApplicationResponse mapToLoanApplicationResponse(LoanApplication app) {

        return LoanApplicationResponse.builder()
                .id(app.getId())
                .applicationReference(app.getApplicationReference())
                .applicantName(app.getUser().getFirstName() + " " + app.getUser().getLastName())
                .accountNumber(app.getAccount().getAccountNumber())
                .branchName(app.getBranch().getBranchName())
                .loanType(app.getLoanType())
                .interestMethod(app.getInterestMethod())
                .requestedAmount(app.getRequestedAmount())
                .tenureMonths(app.getTenureMonths())
                .purpose(app.getPurpose())
                .status(app.getStatus())
                .managerComments(app.getManagerComments())
                .createdAt(app.getCreatedAt())
                .reviewedAt(app.getReviewedAt())
                .build();
    }

    private CreditCardResponse mapToCardResponse(CreditCard card) {

        return CreditCardResponse.builder()
                .id(card.getId())
                .maskedCardNumber(card.getMaskedCardNumber())
                .cardHolderName(card.getCardHolderName())
                .cardType(card.getCardType())
                .status(card.getStatus())
                .creditLimit(card.getCreditLimit())
                .availableLimit(card.getAvailableLimit())
                .outstandingAmount(card.getOutstandingAmount())
                .minimumDue(card.getMinimumDue())
                .totalRewardPoints(card.getTotalRewardPoints())
                .expiryDate(card.getExpiryDate())
                .linkedAccountNumber(card.getLinkedAccount().getAccountNumber())
                .createdAt(card.getCreatedAt())
                .build();
    }

    private BranchTransferResponse mapToTransferResponse(BranchTransferRequest req){

        return BranchTransferResponse.builder()
                .id(req.getId())
                .transferReference(req.getTransferReference())
                .accountNumber(req.getAccount().getAccountNumber())
                .accountHolderName(req.getUser().getFirstName() + " " + req.getUser().getLastName())
                .currentBranchName(req.getCurrentBranch().getBranchName())
                .requestedBranchName(req.getRequestedBranch().getBranchName())
                .reason(req.getReason())
                .status(req.getStatus())
                .currentBranchComments(req.getCurrentBranchComments())
                .newBranchComments(req.getNewBranchComments())
                .currentBranchReviewedAt(req.getCurrentBranchReviewedAt())
                .newBranchReviewedAt(req.getNewBranchReviewedAt())
                .createdAt(req.getCreatedAt())
                .build();
    }
}