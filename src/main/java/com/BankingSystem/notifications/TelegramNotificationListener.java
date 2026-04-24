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
public class TelegramNotificationListener {

    @Async
    @EventListener
    public void handleTransactionNotification(TransactionCompletedEvent event) {
        try {
            String message = buildTelegramMessage(event);

            // TODO: Replace with real Telegram Bot API implementation
            log.info("TELEGRAM SENT TO: {} | MESSAGE: {}",
                    event.getPhoneNumber(), message);

        } catch (Exception e) {
            log.error("Failed to send Telegram notification for transaction: {} | Error: {}",
                    event.getTransaction().getTransactionReference(),
                    e.getMessage());
        }
    }

    private String buildTelegramMessage(TransactionCompletedEvent event) {
        return String.format(
                "🏦 *Banking Alert*%n" +
                "Hello %s%n" +
                "Transaction Type: %s%n" +
                "Amount: ₹%s%n" +
                "Account: %s%n" +
                "Balance: ₹%s%n" +
                "Reference: %s",
                event.getAccountHolderName(),
                event.getTransaction().getTransactionType(),
                event.getTransaction().getAmount(),
                event.getAccount().getAccountNumber(),
                event.getTransaction().getBalanceAfterTransaction(),
                event.getTransaction().getTransactionReference()
        );
    }
}