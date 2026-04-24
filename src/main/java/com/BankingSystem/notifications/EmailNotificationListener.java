package com.BankingSystem.notifications;

import com.BankingSystem.util.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    @Async
    @EventListener
    public void handleTransactionNotification(TransactionCompletedEvent event) {
        try {
            String subject = buildEmailSubject(event);
            String body = buildEmailBody(event);

            // TODO: Replace with real JavaMailSender implementation
            log.info("EMAIL SENT TO: {} | SUBJECT: {} | BODY: {}",
                    event.getEmail(), subject, body);

        } catch (Exception e) {
            log.error("Failed to send email notification for transaction: {} | Error: {}",
                    event.getTransaction().getTransactionReference(),
                    e.getMessage());
        }
    }

    private String buildEmailSubject(TransactionCompletedEvent event) {
        return switch (event.getTransaction().getTransactionType()) {
            case DEPOSIT -> "Money Deposited - " +
                    event.getTransaction().getTransactionReference();
            case WITHDRAWAL -> "Money Withdrawn - " +
                    event.getTransaction().getTransactionReference();
            case TRANSFER_DEBIT -> "Money Sent - " +
                    event.getTransaction().getTransactionReference();
            case TRANSFER_CREDIT -> "Money Received - " +
                    event.getTransaction().getTransactionReference();
        };
    }

    private String buildEmailBody(TransactionCompletedEvent event) {
        return String.format(
                "Dear %s, %s of ₹%s on account %s. " +
                "Balance after transaction: ₹%s. " +
                "Reference: %s.",
                event.getAccountHolderName(),
                event.getTransaction().getTransactionType(),
                event.getTransaction().getAmount(),
                event.getAccount().getAccountNumber(),
                event.getTransaction().getBalanceAfterTransaction(),
                event.getTransaction().getTransactionReference()
        );
    }
}