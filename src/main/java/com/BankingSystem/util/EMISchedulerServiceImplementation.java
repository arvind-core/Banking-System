package com.BankingSystem.util;

import com.BankingSystem.BankConfig;
import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.loan.EMISchedule;
import com.BankingSystem.entity.loan.EMIStatus;
import com.BankingSystem.entity.loan.LoanAccount;
import com.BankingSystem.entity.loan.LoanStatus;
import com.BankingSystem.entity.transactions.Transaction;
import com.BankingSystem.entity.transactions.TransactionStatus;
import com.BankingSystem.entity.transactions.TransactionType;
import com.BankingSystem.repo.AccountRepository;
import com.BankingSystem.repo.EMIScheduleRepository;
import com.BankingSystem.repo.LoanAccountRepository;
import com.BankingSystem.repo.TransactionRepository;
import com.BankingSystem.service.trust.TrustScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.BankingSystem.BankConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class EMISchedulerServiceImplementation implements EMISchedulerService{

    private final EMIScheduleRepository emiScheduleRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TrustScoreService trustScoreService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 0 * * *")
    public void processEMIsDueToday(){
        LocalDate today = LocalDate.now();
        log.info("EMI Scheduler started for date : {}", today);

        List<EMISchedule> emisDueToday = emiScheduleRepository.findEmisDueToday(today);

        log.info("Found {} EMIs due today", emisDueToday.size());

        int successCount = 0;
        int failedCount = 0;

        for (EMISchedule emi : emisDueToday){
            try{
                processIndividualEmi(emi);
                successCount++;
            }catch (Exception e){
                log.error("Failed to process EMI id: {} for loan: {} | Error: {}",
                        emi.getId(),
                        emi.getLoanAccount().getLoanAccountNumber(),
                        e.getMessage());
                failedCount++;
            }
        }

        log.info("EMI Scheduler completed. Success: {} Failed: {}",successCount,failedCount);
    }

    // Runs every day at 10 AM -sends reminders 3 days before due date

    @Scheduled(cron = "0 0 10 * * *")
    public void sendEMIReminders() {

        LocalDate reminderDate = LocalDate.now().plusDays(3);
        List<EMISchedule> upcomingEMIs = emiScheduleRepository.findEmisForReminder(reminderDate);

        log.info("Sending EMI reminders for {} upcoming EMIs", upcomingEMIs.size());

        upcomingEMIs.forEach(emi -> {
            try{
                sendEMIReminderNotification(emi);
            }catch (Exception e){
                log.error("Failed to send reminder for EMI id : {}",emi.getId());
            }
        });
    }

    @Scheduled(cron = "0 0 9 * * *") // 9 AM daily for first attempt
    @Override
    public void processEmisDueToday() {
        LocalDate today = LocalDate.now();
        log.info("EMI Scheduler — first attempt for date: {}", today);

        List<EMISchedule> dueEmis = emiScheduleRepository.findEmisDueToday(today);
        log.info("Found {} EMIs due today", dueEmis.size());

        int successCount = 0;
        int failCount = 0;

        for (EMISchedule emi : dueEmis) {
            try {
                processIndividualEmi(emi);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("First attempt failed for EMI id: {} | Error: {}",
                        emi.getId(), e.getMessage());
            }
        }

        log.info("First attempt complete. Success: {} | Failed: {}",
                successCount, failCount);
    }

    @Scheduled(cron = "0 0 10 * * *")
    @Override
    public void processRetryEmis() {
        LocalDate today = LocalDate.now();
        log.info("EMI Retry Scheduler started for date: {}", today);

        List<EMISchedule> retryEmis = emiScheduleRepository
                .findEmisForRetry(today, BankConfig.EMI_MAX_RETRY_ATTEMPTS);

        log.info("Found {} EMIs eligible for retry", retryEmis.size());

        for (EMISchedule emi : retryEmis) {
            if (emi.getLastRetryDate() != null &&
                    emi.getLastRetryDate().isEqual(today)) {
                continue;
            }
            try {
                processIndividualEmi(emi);
            } catch (Exception e) {
                log.error("Retry failed for EMI id: {} (attempt {}) | Error: {}",
                        emi.getId(), emi.getRetryCount(), e.getMessage());
            }
        }

        LocalDate graceDeadline = today.minusDays(BankConfig.EMI_GRACE_PERIOD_DAYS);
        List<EMISchedule> exhaustedEmis = emiScheduleRepository
                .findEmisExhaustedRetries(graceDeadline,
                        BankConfig.EMI_MAX_RETRY_ATTEMPTS);

        for (EMISchedule emi : exhaustedEmis) {
            try {
                imposePenalty(emi.getId());
            } catch (Exception e) {
                log.error("Failed to impose penalty for EMI id: {} | Error: {}",
                        emi.getId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void imposePenalty(Long emiScheduleId) {
        EMISchedule emi = emiScheduleRepository.findById(emiScheduleId)
                .orElseThrow(() -> new RuntimeException(
                        "EMI not found: " + emiScheduleId));

        if (emi.getStatus() != EMIStatus.PENDING) return;

        BigDecimal penalty = emi.getEmiAmount()
                .multiply(BigDecimal.valueOf(
                        BankConfig.EMI_PENALTY_PERCENTAGE / 100))
                .setScale(2, RoundingMode.HALF_UP);

        emi.setStatus(EMIStatus.MISSED);
        emi.setPenaltyAmount(penalty);
        emiScheduleRepository.save(emi);

        LoanAccount loanAccount = emi.getLoanAccount();

        trustScoreService.decreaseScore(
                loanAccount.getUser().getId(),
                Math.abs(BankConfig.SCORE_EMI_MISSED),
                "EMI missed after " + BankConfig.EMI_MAX_RETRY_ATTEMPTS +
                        " retry attempts");

        Map<String, Object> data = new HashMap<>();
        data.put("emiAmount", emi.getEmiAmount());
        data.put("dueDate", emi.getDueDate().toString());
        data.put("penalty", penalty);
        data.put("retriesAttempted", BankConfig.EMI_MAX_RETRY_ATTEMPTS);
        data.put("loanAccountNumber", loanAccount.getLoanAccountNumber());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.EMI_MISSED,
                loanAccount.getUser().getFirstName() + " " +
                        loanAccount.getUser().getLastName(),
                loanAccount.getUser().getEmail(),
                loanAccount.getUser().getPhoneNumber(),
                data));

        log.warn("Penalty ₹{} imposed for EMI {} after {} failed attempts.",
                penalty, emiScheduleId, BankConfig.EMI_MAX_RETRY_ATTEMPTS);
    }

    @Override
    public void sendEmiReminders() {

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIndividualEmi(EMISchedule emi){
        EMISchedule freshEMI = emiScheduleRepository.findById(emi.getId()).orElseThrow();

        if(freshEMI.getStatus() != EMIStatus.PENDING){
            log.info("EMI {} already processed with status {}. Skipping",freshEMI.getId(),freshEMI.getStatus());
            return;
        }

        LoanAccount loanAccount = freshEMI.getLoanAccount();
        Account disbursementAccount = accountRepository
                .findByAccountNumberWithLock(loanAccount
                        .getDisbursementAccount()
                        .getAccountNumber())
                .orElseThrow();

        if(disbursementAccount.getBalance().compareTo(freshEMI.getEmiAmount()) >= 0){
            processSuccessfulEMI(freshEMI,loanAccount,disbursementAccount);
        }
        else {
            processMissedEMI(freshEMI,loanAccount,disbursementAccount);
        }
    }

    private void processSuccessfulEMI(EMISchedule emi, LoanAccount loanAccount, Account account){

        // deduct EMI from account

        account.setBalance(account.getBalance().subtract(emi.getEmiAmount()));
        accountRepository.save(account);

        // Update EMI status
        emi.setStatus(EMIStatus.PAID);
        emi.setPaidDate(LocalDate.now());
        emiScheduleRepository.save(emi);

        // Update Loan Account

        loanAccount.setOutstandingPrincipal(loanAccount.getOutstandingPrincipal().subtract(emi.getPrincipalComponent()));
        loanAccount.setTotalAmountPaid(loanAccount.getTotalAmountPaid().add(emi.getEmiAmount()));
        loanAccount.setEmisPaid(loanAccount.getEmisPaid() + 1);
        loanAccount.setEmisRemaining(loanAccount.getEmisRemaining() - 1);
        loanAccount.setNextEmiDate(emi.getDueDate().plusMonths(1));

        // Check if loan is fully repaid
        if (loanAccount.getEmisRemaining() == 0){
            loanAccount.setStatus(LoanStatus.CLOSED);
            loanAccount.setClosureDate(LocalDate.now());
            log.info("Loan {} fully repaid and closed",loanAccount.getLoanAccountNumber());

            // Increase Trust Score for Completing loan
            trustScoreService.increaseScore(loanAccount.getUser().getId(), SCORE_LOAN_FULLY_REPAID, ("Loan fully repaid : "+ loanAccount.getLoanAccountNumber()));

            // Send Loan fully repaid notification
            sendLoanFullyRepaidNotification(loanAccount);
        }

        loanAccountRepository.save(loanAccount);

        Transaction transaction = Transaction.builder()
                .transactionReference("EMI-" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.SUCCESS)
                .amount(emi.getEmiAmount())
                .balanceAfterTransaction(account.getBalance())
                .account(account)
                .description("EMI payment - " + loanAccount.getLoanAccountNumber() + "installment " + emi.getInstallmentNumber())
                .build();

        transactionRepository.save(transaction);

        //Increase Trust score for on time payment
        trustScoreService.increaseScore(loanAccount.getUser().getId(),
                SCORE_EMI_PAID_ON_TIME,
                "EMI pai on time for loan: " + loanAccount.getLoanAccountNumber());


        // send success notification
        sendEMISuccessNotification(emi,loanAccount,account.getBalance());

        log.info("EMI processed successfully for loan : {} installment : {} " ,
                loanAccount.getLoanAccountNumber(),
                emi.getInstallmentNumber());
    }

    private void processMissedEMI(EMISchedule emi, LoanAccount loanAccount, Account disbursementAccount) {
        emi.setRetryCount(emi.getRetryCount() + 1);
        emi.setLastRetryDate(LocalDate.now());
        emiScheduleRepository.save(emi);

        Map<String, Object> data = new HashMap<>();
        data.put("emiAmount", emi.getEmiAmount());
        data.put("dueDate", emi.getDueDate().toString());
        data.put("retryCount", emi.getRetryCount());
        data.put("retriesRemaining",
                BankConfig.EMI_MAX_RETRY_ATTEMPTS - emi.getRetryCount());
        data.put("loanAccountNumber", loanAccount.getLoanAccountNumber());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.EMI_MISSED,
                loanAccount.getUser().getFirstName() + " " +
                        loanAccount.getUser().getLastName(),
                loanAccount.getUser().getEmail(),
                loanAccount.getUser().getPhoneNumber(),
                data));

        log.warn("EMI {} deduction failed — insufficient balance. " +
                        "Attempt {}/{}. Will retry tomorrow.",
                emi.getId(), emi.getRetryCount(),
                BankConfig.EMI_MAX_RETRY_ATTEMPTS);
    }

    private void sendEMISuccessNotification(EMISchedule emi, LoanAccount loanAccount, BigDecimal remainingBalance){

        Map<String, Object> data = new HashMap<>();

        data.put("emiAmount",emi.getEmiAmount());
        data.put("installmentNumber", emi.getInstallmentNumber());
        data.put("loanAccountNumber",loanAccount.getLoanAccountNumber());
        data.put("outStandingPrincipal",loanAccount.getOutstandingPrincipal());
        data.put("emisRemaining",loanAccount.getEmisRemaining());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.EMI_DEDUCTED,
                loanAccount.getUser().getFirstName() + " " + loanAccount.getUser().getLastName(),
                loanAccount.getUser().getEmail(),
                loanAccount.getUser().getPhoneNumber(),
                data));
    }

    private void sendEMIMissedNotification(EMISchedule emi, LoanAccount loanAccount, BigDecimal penalty){

        Map<String, Object> data = new HashMap<>();
        data.put("emiAmount", emi.getEmiAmount());
        data.put("dueDate",emi.getDueDate());
        data.put("penalty",penalty);
        data.put("loanAccountNumber",loanAccount.getLoanAccountNumber());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.EMI_MISSED,
                loanAccount.getUser().getFirstName() + " " + loanAccount.getUser().getLastName(),
                loanAccount.getUser().getEmail(),
                loanAccount.getUser().getPhoneNumber(),
                data));
    }

    private void sendLoanFullyRepaidNotification(LoanAccount loanAccount){

        Map<String, Object> data = new HashMap<>();

        data.put("loanAccountNumber",loanAccount.getLoanAccountNumber());
        data.put("loanType",loanAccount.getLoanType());
        data.put("totalAmoutnPaid",loanAccount.getTotalAmountPaid());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.LOAN_FULLY_REPAID,
                loanAccount.getUser().getFirstName() + " " + loanAccount.getUser().getLastName(),
                loanAccount.getUser().getEmail(),
                loanAccount.getUser().getPhoneNumber(),
                data));
    }

    private void sendEMIReminderNotification(EMISchedule emi){

        Map<String, Object> data = new HashMap<>();

        data.put("emiAmount",emi.getEmiAmount());
        data.put("dueDate",emi.getDueDate().toString());
        data.put("loanAccountNumber",emi.getLoanAccount().getLoanAccountNumber());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.EMI_DUE_REMINDER,
                emi.getLoanAccount().getUser().getFirstName() + " " + emi.getLoanAccount().getUser().getLastName(),
                emi.getLoanAccount().getUser().getEmail(),
                emi.getLoanAccount().getUser().getPhoneNumber(),
                data));

    }
























}
