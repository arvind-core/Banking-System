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
public class SMSNotificationListener {

    @Async
    @EventListener
    public void handleTransactionNotification(TransactionCompletedEvent event) {
        try {
            String message = buildSMSMessage(event);

            // TODO: Replace with real Twilio implementation
            log.info("SMS SENT TO: {} | MESSAGE: {}",
                    event.getPhoneNumber(), message);

        } catch (Exception e) {
            log.error("Failed to send SMS notification for transaction: {} | Error: {}",
                    event.getTransaction().getTransactionReference(),
                    e.getMessage());
        }
    }

    private String buildSMSMessage(TransactionCompletedEvent event) {
        return switch (event.getTransaction().getTransactionType()) {
            case DEPOSIT -> String.format(
                    "₹%s deposited to account %s. Balance: ₹%s. Ref: %s",
                    event.getTransaction().getAmount(),
                    event.getAccount().getAccountNumber(),
                    event.getTransaction().getBalanceAfterTransaction(),
                    event.getTransaction().getTransactionReference());
            case WITHDRAWAL -> String.format(
                    "₹%s withdrawn from account %s. Balance: ₹%s. Ref: %s",
                    event.getTransaction().getAmount(),
                    event.getAccount().getAccountNumber(),
                    event.getTransaction().getBalanceAfterTransaction(),
                    event.getTransaction().getTransactionReference());
            case TRANSFER_DEBIT -> String.format(
                    "₹%s sent from account %s to %s. Balance: ₹%s. Ref: %s",
                    event.getTransaction().getAmount(),
                    event.getAccount().getAccountNumber(),
                    event.getTransaction().getTargetAccountNumber(),
                    event.getTransaction().getBalanceAfterTransaction(),
                    event.getTransaction().getTransactionReference());
            case TRANSFER_CREDIT -> String.format(
                    "₹%s received in account %s from %s. Balance: ₹%s. Ref: %s",
                    event.getTransaction().getAmount(),
                    event.getAccount().getAccountNumber(),
                    event.getTransaction().getTargetAccountNumber(),
                    event.getTransaction().getBalanceAfterTransaction(),
                    event.getTransaction().getTransactionReference());
        };
    }
}