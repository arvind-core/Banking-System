package com.BankingSystem.service.branchTransfer;

import com.BankingSystem.dto.request.branchTransfer.BranchTransferCreate;
import com.BankingSystem.dto.request.branchTransfer.BranchTransferReviewRequest;
import com.BankingSystem.dto.response.branchTransfer.BranchTransferResponse;
import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.bank.Branch;
import com.BankingSystem.entity.bank.BranchTransferRequest;
import com.BankingSystem.entity.loan.LoanAccount;
import com.BankingSystem.entity.loan.LoanStatus;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.InvalidOperationException;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.*;
import com.BankingSystem.util.NotificationEvent;
import com.BankingSystem.util.NotificationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.BankingSystem.entity.bank.BranchTransferStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchTransferServiceImpl implements BranchTransferService {

    private final BranchTransferRequestRepository transferRepository;
    private final AccountRepository accountRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Override
    @Transactional
    public BranchTransferResponse requestTransfer(BranchTransferCreate request, Long userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User Not found : " + userId));

        Account account = accountRepository.findByAccountNumberAndIsActiveTrue(request.getAccountNumber()).orElseThrow(() -> new ResourceNotFoundException("Account Not found : " + request.getAccountNumber()));

        // Verify account belongs to user
        if(!account.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("Account does not belong to you.");
        }

        // Check no active transfer already pending
        if(transferRepository.hasActivePendingTransfer(request.getAccountNumber())) {
            throw new InvalidOperationException("Account already has a pending branch transfer request.");
        }

        Branch currentBranch = account.getBranch();
        Branch reqeustedBranch = branchRepository.findByBranchCode(request.getRequestedBranchCode()).orElseThrow(() -> new ResourceNotFoundException("Branch Not found : " + request.getRequestedBranchCode()));

        if(!reqeustedBranch.isActive()){
            throw new InvalidOperationException("Requested Branch is currently inactive.");
        }

        if(currentBranch.getId().equals(reqeustedBranch.getId())){
            throw new InvalidOperationException("Account is already in this branch.");
        }

        BranchTransferRequest transferRequest = BranchTransferRequest.builder()
                .transferReference("BTR-" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .account(account)
                .user(user)
                .currentBranch(currentBranch)
                .requestedBranch(reqeustedBranch)
                .reason(request.getReason())
                .status(PENDING_CURRENT_BRANCH_APPROVAL)
                .build();

        BranchTransferRequest saved = transferRepository.save(transferRequest);

        // Notify User - request submitted
        Map<String, Object> data = new HashMap<>();
        data.put("transferReference", saved.getTransferReference());
        data.put("currentBranch", currentBranch.getBranchName());
        data.put("requestedBranch", reqeustedBranch.getBranchName());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.BRANCH_TRANSFER_REQUESTED,
                user.getFirstName() + "  " + user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                data));

        log.info("Branch transfer request {} created for account {} from {} to {}.",saved.getTransferReference(), account.getAccountNumber(),currentBranch.getBranchName(),reqeustedBranch.getBranchName());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public BranchTransferResponse currentBranchReview(BranchTransferReviewRequest request) {
        BranchTransferRequest transferRequest = transferRepository.findByTransferReference(request.getTransferReference()).orElseThrow(() -> new ResourceNotFoundException("Transfer request not found : " + request.getTransferReference()));

        if(transferRequest.getStatus() != PENDING_CURRENT_BRANCH_APPROVAL){
            throw new InvalidOperationException("Request is not in pending current branch approval. Current Status : " + transferRequest.getStatus());
        }

        // Idempotency - already reviewed
        if(transferRequest.getCurrentBranchReviewedAt() != null){
            log.info("Transfer {} already reviewed by current branch. Skipping.",request.getTransferReference());
            return mapToResponse(transferRequest);
        }

        User manager = userRepository.findById(request.getManagerUserId()).orElseThrow(() -> new ResourceNotFoundException("Manager Not found : " + request.getManagerUserId()));

        transferRequest.setCurrentBranchReviewedBy(manager);
        transferRequest.setCurrentBranchComments(request.getComments());
        transferRequest.setCurrentBranchReviewedAt(LocalDateTime.now());

        if(request.getApproved()){
            // Move to next stage - waiting for new branch
            transferRequest.setStatus(PENDING_CURRENT_BRANCH_APPROVAL);

            log.info("Transfer {} approved by current branch {}. Awaiting new branch Approval.",request.getTransferReference(),transferRequest.getCurrentBranch().getBranchName());
        }
        else{
            transferRequest.setStatus(REJECTED_BY_CURRENT_BRANCH);

            // Notify user of rejection
            notifyTransferRejection(transferRequest, "current branch: " + request.getComments());

            log.info("Transfer {} Rejected by current branch {}.", request.getTransferReference(), transferRequest.getCurrentBranch().getBranchName());
        }

        BranchTransferRequest updated = transferRepository.save(transferRequest);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public BranchTransferResponse newBranchReview(BranchTransferReviewRequest request) {

        BranchTransferRequest transferRequest = transferRepository.findByTransferReference(request.getTransferReference()).orElseThrow(() -> new ResourceNotFoundException("Transfer request not found : " + request.getTransferReference()));

        if(transferRequest.getStatus() != PENDING_CURRENT_BRANCH_APPROVAL){
            throw new InvalidOperationException("Request is not pending new branch approval. Current Status : " + transferRequest.getStatus());
        }

        // Idempotency - already reviewed
        if(transferRequest.getCurrentBranchReviewedBy() != null){
            log.info("Transfer {} already reviewed by new branch. Skipping.",request.getTransferReference());
            return mapToResponse(transferRequest);
        }

        User manager = userRepository.findById(request.getManagerUserId()).orElseThrow(() -> new ResourceNotFoundException("Manager Not found : " + request.getManagerUserId()));

        transferRequest.setNewBranchReviewedBy(manager);
        transferRequest.setNewBranchComments(request.getComments());
        transferRequest.setNewBranchReviewedAt(LocalDateTime.now());

        if(request.getApproved()){
            transferRequest.setStatus(APPROVED);

            // Execute teh actual branch transfer
            executeBranchTransfer(transferRequest);
            log.info("Transfer {} fully approved. Account {} moved from {} to {}.", request.getTransferReference(), transferRequest.getAccount().getAccountNumber(), transferRequest.getCurrentBranch().getBranchName(), transferRequest.getRequestedBranch().getBranchName());
        }
        else{
            transferRequest.setStatus(REJECTED_BY_NEW_BRANCH);
            notifyTransferRejection(transferRequest, "new branch : " + request.getComments());
            log.info("Transfer {} rejected by new branch {}.", request.getTransferReference(), transferRequest.getRequestedBranch().getBranchName());
        }

        BranchTransferRequest updated = transferRepository.save(transferRequest);
        return mapToResponse(updated);
    }

    @Override
    public List<BranchTransferResponse> getUserTransferHistory(Long userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User Not found : " + userId));

        return transferRepository.findByUser(user)
                .stream()
                .map(this :: mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BranchTransferResponse> getPendingForCurrentBranch(Long branchId) {

        Branch branch =  branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch not found : " + branchId));

        return transferRepository.findPendingForCurrentBranch(branch)
                .stream()
                .map(this :: mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BranchTransferResponse> getPendingForNewBranch(Long branchId) {

        Branch branch =   branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch not found : " + branchId));

        return transferRepository.findPendingForNewBranch(branch)
                .stream()
                .map(this :: mapToResponse)
                .collect(Collectors.toList());
    }

    private void executeBranchTransfer(BranchTransferRequest transferRequest) {
        Account account = transferRequest.getAccount();
        Branch newBranch = transferRequest.getRequestedBranch();

        // Update account branch
        account.setBranch(newBranch);
        accountRepository.save(account);

        // Update currentBranch on all active loans for this account
        // Originating Branch stays unchanged - historical record
        List<LoanAccount> activeLoans = loanAccountRepository.findByUserAndStatus(account.getUser(), LoanStatus.ACTIVE);

        for(LoanAccount loan : activeLoans){
            // Only update loans linked to this specific account
            if(loan.getDisbursementAccount().getId().equals(account.getId())){
                loan.setCurrentBranch(newBranch);
                loanAccountRepository.save(loan);
                log.info("Loan {} current Branch updated to {} for transfer.", loan.getLoanAccountNumber(), newBranch.getBranchName());
            }
        }

        // Notify user - transfer complete
        Map<String,Object> data = new HashMap<>();
        data.put("transferReference", transferRequest.getTransferReference());
        data.put("newBranch", newBranch);
        data.put("newBranchCode", newBranch.getBranchCode());
        data.put("newIfscCode", newBranch.getIfscCode());
        data.put("accouNtNumber", account.getAccountNumber());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.BRANCH_TRANSFER_COMPLETED,
                account.getUser().getFirstName() + " " + account.getUser().getLastName(),
                account.getUser().getEmail(),
                account.getUser().getPhoneNumber(),
                data));
    }

    private void notifyTransferRejection(BranchTransferRequest transferRequest, String reason) {
        Map<String,Object> data = new HashMap<>();
        data.put("transferReference", transferRequest.getTransferReference());
        data.put("reason", reason);
        data.put("currentBranch", transferRequest.getCurrentBranch());
        data.put("requestedBranch", transferRequest.getRequestedBranch());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.BRANCH_TRANSFER_REJECTED,
                transferRequest.getUser().getFirstName() + " " + transferRequest.getUser().getLastName(),
                transferRequest.getUser().getEmail(),
                transferRequest.getUser().getPhoneNumber(),
                data));
    }

    private BranchTransferResponse mapToResponse(BranchTransferRequest req) {

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
