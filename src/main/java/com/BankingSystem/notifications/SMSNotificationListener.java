package com.BankingSystem.notifications;

import com.BankingSystem.util.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static com.BankingSystem.Akash.BANK_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class SMSNotificationListener {

    @Async
    @EventListener
    public void handleNotification(NotificationEvent event) {
        try {
            String message = buildSMSMessage(event);

            // TODO: Replace with real Twilio implementation
            log.info("SMS → TO: {} | MESSAGE: {}",
                    event.getRecipientPhone(), message);

        } catch (Exception e) {
            log.error("Failed to send SMS for event: {} | Error: {}",
                    event.getEventType(), e.getMessage());
        }
    }

    private String buildSMSMessage(NotificationEvent event) {
        return switch (event.getEventType()) {
            case DEPOSIT -> String.format(
                    BANK_NAME + ": ₹%s credited to account %s. Bal: ₹%s. Ref: %s",
                    event.getStringData("amount"),
                    event.getStringData("accountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case WITHDRAWAL -> String.format(
                    BANK_NAME + ": ₹%s debited from account %s. Bal: ₹%s. Ref: %s",
                    event.getStringData("amount"),
                    event.getStringData("accountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case TRANSFER_SENT -> String.format(
                    BANK_NAME + ": ₹%s sent to account %s. Bal: ₹%s. Ref: %s",
                    event.getStringData("amount"),
                    event.getStringData("targetAccountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case TRANSFER_RECEIVED -> String.format(
                    BANK_NAME + ": ₹%s received from account %s. Bal: ₹%s. Ref: %s",
                    event.getStringData("amount"),
                    event.getStringData("targetAccountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case ACCOUNT_CREATED -> String.format(
                    BANK_NAME + ": Account %s created successfully. Welcome!",
                    event.getStringData("accountNumber"));
            case LOAN_APPROVED -> String.format(
                    BANK_NAME + ": Loan of ₹%s approved. EMI: ₹%s/month.",
                    event.getStringData("loanAmount"),
                    event.getStringData("emiAmount"));
            case EMI_DUE_REMINDER -> String.format(
                    BANK_NAME + ": EMI of ₹%s due on %s. Please maintain sufficient balance.",
                    event.getStringData("emiAmount"),
                    event.getStringData("dueDate"));
            case EMI_MISSED -> String.format(
                    BANK_NAME + ": EMI of ₹%s missed. Penalty: ₹%s applied. Pay immediately.",
                    event.getStringData("emiAmount"),
                    event.getStringData("penalty"));
            case LOGIN_SUCCESSFUL -> String.format(
                    BANK_NAME + ": New login detected on your account. Not you? Call us immediately.");
            default -> String.format(
                   BANK_NAME + ":%s. Contact support for details.",
                    event.getEventType().toString().replace("_", " ").toLowerCase());
        };
    }
}