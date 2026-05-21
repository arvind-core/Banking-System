package com.BankingSystem.service.money_request;

import com.BankingSystem.dto.request.moeny_request.MoneyRequestCreate;
import com.BankingSystem.dto.response.money_request_response.MoneyRequestResponse;
import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.requests.MoneyRequest;
import com.BankingSystem.entity.requests.MoneyRequestStatus;
import com.BankingSystem.entity.transactions.Transaction;
import com.BankingSystem.entity.transactions.TransactionStatus;
import com.BankingSystem.entity.transactions.TransactionType;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.InvalidOperationException;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.AccountRepository;
import com.BankingSystem.repo.MoneyRequestRepository;
import com.BankingSystem.repo.TransactionRepository;
import com.BankingSystem.repo.UserRepository;
import com.BankingSystem.util.notifications.NotificationEvent;
import com.BankingSystem.util.notifications.NotificationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.BankingSystem.BankConfig.AUTO_EXPIRE_MONEY_REQUEST_HOURS;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoneyRequestServiceImplementation implements MoneyRequestService {

    private final MoneyRequestRepository moneyRequestRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public MoneyRequestResponse createRequest(MoneyRequestCreate request, Long requesterId) {

        User requester = userRepository.findById(requesterId).orElseThrow(() -> new ResourceNotFoundException("Requester not found : " + requesterId));

        // Find payer by Phone number
        User payer = userRepository.findByPhoneNumberAndIsActiveTrue(request.getPayerPhoneNumber()).orElseThrow(() -> new ResourceNotFoundException("No user found with phone number : " + request.getPayerPhoneNumber()));

        // Can not request from yourself
        if(requester.getId().equals(payer.getId())) {
            throw new InvalidOperationException("Can not send money request to yourself");
        }

        Account requesterAccount = accountRepository.findByAccountNumberAndIsActiveTrue(request.getRequesterAccountNumber()).orElseThrow(() -> new ResourceNotFoundException("Requester account not found : " + request.getRequesterAccountNumber()));

        // Verify account belongs to requester
        if(!requesterAccount.getId().equals(requesterId)) {
            throw new InvalidOperationException("Account does not belong to requester");
        }

        MoneyRequest moneyRequest = MoneyRequest.builder()
                .requestReference("REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .requester(requester)
                .payer(payer)
                .requesterAccount(requesterAccount)
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(MoneyRequestStatus.PENDING)
                .expiresAt(LocalDate.now().atStartOfDay().plusHours(AUTO_EXPIRE_MONEY_REQUEST_HOURS))
                .build();

        MoneyRequest saved = moneyRequestRepository.save(moneyRequest);

        // Notify Payer
        Map<String, Object> data = new HashMap<>();
        data.put("requesterName", requester.getFirstName() + " " + requester.getLastName());
        data.put("amount", request.getAmount());
        data.put("description", request.getDescription());
        data.put("requestReference", saved.getRequestReference());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.MONEY_REQUEST_RECEIVED,
                payer.getFirstName() + " " + payer.getLastName(),
                payer.getEmail(),
                payer.getPhoneNumber(),
                data));

        log.info("Money request {} created by user {} for user {}. Amount : ₹{}", saved.getRequestReference(), requesterId, payer.getId(), request.getAmount());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public MoneyRequestResponse acceptRequest(String requestReference, Long payerUserId) {

        MoneyRequest moneyRequest = moneyRequestRepository.findByRequestReference(requestReference).orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestReference));

        // Validate payer
        if(!moneyRequest.getPayer().getId().equals(payerUserId)) {
            throw new InvalidOperationException("This request is not addressed to you");
        }

        if(moneyRequest.getStatus() != MoneyRequestStatus.PENDING) {
            throw new InvalidOperationException("Request is no longer pending. Current status : " + moneyRequest.getStatus());
        }

        if(LocalDateTime.now().isAfter(moneyRequest.getExpiresAt())) {
            moneyRequest.setStatus(MoneyRequestStatus.EXPIRED);
            moneyRequestRepository.save(moneyRequest);

            throw new InvalidOperationException("This money request has expired.");
        }

        // Get payer's primary account with lock
        Account payerAccount = accountRepository.findPrimaryAccountByUserPhoneNumber(moneyRequest.getPayer().getPhoneNumber()).orElseThrow(() -> new ResourceNotFoundException("No primary account found for payer."));

        Account payerLocked = accountRepository.findByAccountNumberWithLock(payerAccount.getAccountNumber()).orElseThrow(() -> new ResourceNotFoundException("Payer account not found."));

        Account requesterLocked = accountRepository.findByAccountNumberWithLock(moneyRequest.getRequesterAccount().getAccountNumber()).orElseThrow(() -> new ResourceNotFoundException("Requester account not found."));

        // Balance check
        if(payerLocked.getBalance().compareTo(moneyRequest.getAmount()) < 0){
            throw new InvalidOperationException("Insufficient balance to fulfill this request.");
        }

        // Consistent lock ordering - same deadlock preventions transfers
        // Already handled by fetching both accounts, transfer logic below is safe
        // Execute transfer

        payerLocked.setBalance(payerLocked.getBalance().subtract(moneyRequest.getAmount()));
        requesterLocked.setBalance(requesterLocked.getBalance().add(moneyRequest.getAmount()));

        // Record transactions
        String ref = "REQ-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        transactionRepository.save(Transaction.builder()
                .transactionReference(ref + "-DR")
                .transactionType(TransactionType.TRANSFER_DEBIT)
                .status(TransactionStatus.SUCCESS)
                .amount(moneyRequest.getAmount())
                .balanceAfterTransaction(payerLocked.getBalance())
                .account(payerLocked)
                .targetAccountNumber(requesterLocked.getAccountNumber())
                .description("Money Request Payment: " + moneyRequest.getDescription())
                .build());

        transactionRepository.save(Transaction.builder()
                .transactionReference(ref + "-CR")
                .transactionType(TransactionType.TRANSFER_CREDIT)
                .status(TransactionStatus.SUCCESS)
                .amount(moneyRequest.getAmount())
                .balanceAfterTransaction(requesterLocked.getBalance())
                .account(requesterLocked)
                .targetAccountNumber(payerLocked.getAccountNumber())
                .description("Money request received: " + moneyRequest.getDescription())
                .build());

        // Update request status
        moneyRequest.setStatus(MoneyRequestStatus.ACCEPTED);
        moneyRequest.setRespondedAt(LocalDateTime.now());
        MoneyRequest updated = moneyRequestRepository.save(moneyRequest);

        // Notify requester
        Map<String, Object> data = new HashMap<>();
        data.put("payerName" , moneyRequest.getPayer().getFirstName() + " " + moneyRequest.getPayer().getLastName());
        data.put("amount", moneyRequest.getAmount());
        data.put("requestReference", requestReference);

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.MONEY_REQUEST_ACCEPTED,
                moneyRequest.getRequester().getFirstName() + " " + moneyRequest.getRequester().getLastName(),
                moneyRequest.getRequester().getEmail(),
                moneyRequest.getRequester().getPhoneNumber(),
                data));

        log.info("Money request {} accepted. ₹{} transferred.", requestReference, moneyRequest.getAmount());

        return mapToResponse(updated);
    }
    public MoneyRequestResponse rejectRequest(String requestReference, Long payerUserId) {

        MoneyRequest moneyRequest = moneyRequestRepository.findByRequestReference(requestReference).orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestReference));

        if(!moneyRequest.getPayer().getId().equals(payerUserId)) {
            throw new InvalidOperationException("This request is not addressed to you.");
        }

        if(moneyRequest.getStatus() != MoneyRequestStatus.PENDING) {
            throw new InvalidOperationException("Request is no longer pending");
        }

        moneyRequest.setStatus(MoneyRequestStatus.REJECTED);
        moneyRequest.setRespondedAt(LocalDateTime.now());
        MoneyRequest updated = moneyRequestRepository.save(moneyRequest);

        // Notify requester
        Map<String, Object> data = new HashMap<>();
        data.put("payerName",moneyRequest.getPayer().getFirstName() + " " + moneyRequest.getPayer().getLastName());
        data.put("amount", moneyRequest.getAmount());
        data.put("requestReference", requestReference);

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.MONEY_REQUEST_REJECTED,
                moneyRequest.getRequester().getFirstName() + " " + moneyRequest.getRequester().getLastName(),
                moneyRequest.getRequester().getEmail(),
                moneyRequest.getRequester().getPhoneNumber(),
                data));

        return mapToResponse(updated);
    }

    @Override
    public List<MoneyRequestResponse> getIncomingRequests(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found." + userId));

        return moneyRequestRepository
                .findByPayerOrderByCreatedAtDesc(user)
                .stream()
                .map(this :: mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MoneyRequestResponse> getSentRequests(Long userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found." + userId));

        return moneyRequestRepository
                .findByRequesterOrderByCreatedAtDesc(user)
                .stream()
                .map(this :: mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireOldRequests() {
        List<MoneyRequest> expired = moneyRequestRepository.findExpiredRequests(LocalDateTime.now());

        log.info("Found {} expired money requests", expired.size());

        for(MoneyRequest req : expired) {
            req.setStatus(MoneyRequestStatus.EXPIRED);
            moneyRequestRepository.save(req);

            Map<String, Object> data = new HashMap<>();
            data.put("amount", req.getAmount());
            data.put("payerName",req.getPayer().getFirstName() + " " + req.getPayer().getLastName());
            data.put("requestReference", req.getRequestReference());

            eventPublisher.publishEvent(NotificationEvent.forUser(this,
                    NotificationEventType.MONEY_REQUEST_EXPIRED,
                    req.getRequester().getFirstName() + " " + req.getRequester().getLastName(),
                    req.getRequester().getEmail(),
                    req.getRequester().getPhoneNumber(),
                    data));
        }
    }

    private MoneyRequestResponse mapToResponse(MoneyRequest req) {
        return MoneyRequestResponse.builder()
                .id(req.getId())
                .requestReference(req.getRequestReference())
                .requesterName(req.getRequester().getFirstName() + " " + req.getRequester().getLastName())
                .requesterAccountNumber(req.getRequesterAccount().getAccountNumber())
                .payerName(req.getPayer().getFirstName() + " " + req.getPayer().getLastName())
                .amount(req.getAmount())
                .description(req.getDescription())
                .status(req.getStatus())
                .expiresAt(req.getExpiresAt())
                .respondedAt(req.getRespondedAt())
                .createdAt(req.getCreatedAt())
                .build();
    }
}