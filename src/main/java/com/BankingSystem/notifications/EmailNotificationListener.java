package com.BankingSystem.notifications;

import com.BankingSystem.repo.NotificationPreferenceRepository;
import com.BankingSystem.repo.UserRepository;
import com.BankingSystem.util.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static com.BankingSystem.BankConfig.BANK_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserRepository userRepository;

    @Async
    @EventListener
    public void handleNotification(NotificationEvent event) {
        try {
            // Check if user has email enabled
            notificationPreferenceRepository
                    .findByUser(userRepository
                            .findByEmailAndIsActiveTrue(event.getRecipientEmail())
                            .orElse(null))
                            .ifPresent(pref -> {
                                if (pref.isEmailEnabled()) {
                                String subject = buildSubject(event);
                                String body = buildBody(event);
                                // TODO: Replace with real JavaMailSender
                                log.info("EMAIL → TO: {} | SUBJECT: {} | BODY: {}",
                                    event.getRecipientEmail(), subject, body);
                            }
                    });
        } catch (Exception e) {
            log.error("Failed to send email for event: {} | Error: {}",
                    event.getEventType(), e.getMessage());
        }
    }

    private String buildSubject(NotificationEvent event) {
        return switch (event.getEventType()) {
            case DEPOSIT -> "Money Deposited - " + event.getStringData("transactionReference");
            case WITHDRAWAL -> "Money Withdrawn - " + event.getStringData("transactionReference");
            case TRANSFER_SENT -> "Money Sent - " + event.getStringData("transactionReference");
            case TRANSFER_RECEIVED -> "Money Received - " + event.getStringData("transactionReference");
            case ACCOUNT_CREATED -> "Welcome to " + BANK_NAME +"- Account Created Successfully";
            case ACCOUNT_CLOSED -> "Account Closed Successfully";
            case PRIMARY_ACCOUNT_UPDATED -> "Primary Account Updated";
            case LOGIN_SUCCESSFUL -> "New Login Detected on Your Account";
            case PASSWORD_CHANGED -> "Your Password Has Been Changed";
            case EMAIL_UPDATED -> "Your Email Has Been Updated";
            case PHONE_UPDATED -> "Your Phone Number Has Been Updated";
            case LOAN_APPLICATION_SUBMITTED -> "Loan Application Submitted Successfully";
            case LOAN_UNDER_REVIEW -> "Your Loan Application is Under Review";
            case LOAN_APPROVED -> "Congratulations! Your Loan is Approved";
            case LOAN_REJECTED -> "Loan Application Update";
            case LOAN_DISBURSED -> "Loan Amount Disbursed to Your Account";
            case EMI_DUE_REMINDER -> "EMI Due Reminder";
            case EMI_DEDUCTED -> "EMI Deducted Successfully";
            case EMI_MISSED -> "Missed EMI Payment Alert";
            case LOAN_FULLY_REPAID -> "Congratulations! Loan Fully Repaid";
            case CARD_APPLICATION_SUBMITTED -> "Credit Card Application Submitted";
            case CARD_APPROVED -> "Your Credit Card is Approved";
            case CARD_REJECTED -> "Credit Card Application Update";
            case CARD_ACTIVATED -> "Your Credit Card is Now Active";
            case CARD_BLOCKED -> "Your Credit Card Has Been Blocked";
            case BILLING_CYCLE_GENERATED -> "Your Monthly Statement is Ready";
            case PAYMENT_DUE_REMINDER -> "Credit Card Payment Due Reminder";
            case CARD_PAYMENT_RECEIVED -> "Credit Card Payment Received";
            case MONEY_REQUEST_RECEIVED -> "You Have a New Money Request";
            case MONEY_REQUEST_ACCEPTED -> "Money Request Accepted";
            case MONEY_REQUEST_REJECTED -> "Money Request Rejected";
            case MONEY_REQUEST_EXPIRED -> "Money Request Expired";
        };
    }

    private String buildBody(NotificationEvent event) {
        return switch (event.getEventType()) {
            case DEPOSIT -> String.format(
                    "Dear %s, ₹%s has been deposited to account %s. " +
                            "Available balance: ₹%s. Reference: %s.",
                    event.getRecipientName(),
                    event.getStringData("amount"),
                    event.getStringData("accountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case WITHDRAWAL -> String.format(
                    "Dear %s, ₹%s has been withdrawn from account %s. " +
                            "Available balance: ₹%s. Reference: %s.",
                    event.getRecipientName(),
                    event.getStringData("amount"),
                    event.getStringData("accountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case TRANSFER_SENT -> String.format(
                    "Dear %s, ₹%s has been sent from account %s to account %s. " +
                            "Available balance: ₹%s. Reference: %s.",
                    event.getRecipientName(),
                    event.getStringData("amount"),
                    event.getStringData("accountNumber"),
                    event.getStringData("targetAccountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case TRANSFER_RECEIVED -> String.format(
                    "Dear %s, ₹%s has been received in account %s from account %s. " +
                            "Available balance: ₹%s. Reference: %s.",
                    event.getRecipientName(),
                    event.getStringData("amount"),
                    event.getStringData("accountNumber"),
                    event.getStringData("targetAccountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case ACCOUNT_CREATED -> String.format(
                    "Dear %s, your account %s has been created successfully. " +
                            "Account type: %s. Branch: %s. Welcome to NexaBank!",
                    event.getRecipientName(),
                    event.getStringData("accountNumber"),
                    event.getStringData("accountType"),
                    event.getStringData("branchName"));
            case LOAN_APPROVED -> String.format(
                    "Dear %s, your loan of ₹%s has been approved. " +
                            "EMI: ₹%s per month for %s months. Disbursement in progress.",
                    event.getRecipientName(),
                    event.getStringData("loanAmount"),
                    event.getStringData("emiAmount"),
                    event.getStringData("tenure"));
            case EMI_DUE_REMINDER -> String.format(
                    "Dear %s, your EMI of ₹%s is due on %s. " +
                            "Please ensure sufficient balance in your account.",
                    event.getRecipientName(),
                    event.getStringData("emiAmount"),
                    event.getStringData("dueDate"));
            case EMI_MISSED -> String.format(
                    "Dear %s, your EMI of ₹%s due on %s was missed. " +
                            "A penalty of ₹%s has been applied. Please pay immediately.",
                    event.getRecipientName(),
                    event.getStringData("emiAmount"),
                    event.getStringData("dueDate"),
                    event.getStringData("penalty"));
            default -> String.format(
                    "Dear %s, %s. Please contact support if you have any questions.",
                    event.getRecipientName(),
                    event.getEventType().toString().replace("_", " ").toLowerCase());
        };
    }
}