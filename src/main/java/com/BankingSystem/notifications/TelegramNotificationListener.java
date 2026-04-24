package com.BankingSystem.notifications;

import com.BankingSystem.util.NotificationEvent;
import static com.BankingSystem.Akash.BANK_NAME;

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
    public void handleNotification(NotificationEvent event) {
        try {
            String message = buildTelegramMessage(event);

            // TODO: Replace with real Telegram Bot API implementation
            log.info("TELEGRAM → TO: {} | MESSAGE: {}",
                    event.getRecipientPhone(), message);

        } catch (Exception e) {
            log.error("Failed to send Telegram notification for event: {} | Error: {}",
                    event.getEventType(), e.getMessage());
        }
    }

    private String buildTelegramMessage(NotificationEvent event) {
        return switch (event.getEventType()) {
            case DEPOSIT -> String.format(
                    "🏦 + " + BANK_NAME + " Alert*%n" +
                            "✅ Money Received%n" +
                            "👤 %s%n" +
                            "💰 Amount: ₹%s%n" +
                            "🏧 Account: %s%n" +
                            "💳 Balance: ₹%s%n" +
                            "🔖 Ref: %s",
                    event.getRecipientName(),
                    event.getStringData("amount"),
                    event.getStringData("accountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case WITHDRAWAL -> String.format(
                    "🏦" + BANK_NAME + " Alert*%n" +
                            "💸 Money Withdrawn%n" +
                            "👤 %s%n" +
                            "💰 Amount: ₹%s%n" +
                            "🏧 Account: %s%n" +
                            "💳 Balance: ₹%s%n" +
                            "🔖 Ref: %s",
                    event.getRecipientName(),
                    event.getStringData("amount"),
                    event.getStringData("accountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case TRANSFER_SENT -> String.format(
                    "🏦 " + BANK_NAME + " Alert*%n" +
                            "📤 Money Sent%n" +
                            "👤 %s%n" +
                            "💰 Amount: ₹%s%n" +
                            "➡️ To: %s%n" +
                            "💳 Balance: ₹%s%n" +
                            "🔖 Ref: %s",
                    event.getRecipientName(),
                    event.getStringData("amount"),
                    event.getStringData("targetAccountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case TRANSFER_RECEIVED -> String.format(
                    "🏦 " + BANK_NAME + " Alert*%n" +
                            "📥 Money Received%n" +
                            "👤 %s%n" +
                            "💰 Amount: ₹%s%n" +
                            "⬅️ From: %s%n" +
                            "💳 Balance: ₹%s%n" +
                            "🔖 Ref: %s",
                    event.getRecipientName(),
                    event.getStringData("amount"),
                    event.getStringData("targetAccountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case LOAN_APPROVED -> String.format(
                    "🏦 " + BANK_NAME + " Alert*%n" +
                            "✅ Loan Approved!%n" +
                            "👤 %s%n" +
                            "💰 Amount: ₹%s%n" +
                            "📅 EMI: ₹%s/month%n" +
                            "⏳ Tenure: %s months",
                    event.getRecipientName(),
                    event.getStringData("loanAmount"),
                    event.getStringData("emiAmount"),
                    event.getStringData("tenure"));
            case EMI_MISSED -> String.format(
                    "🏦 " + BANK_NAME + " Alert*%n" +
                            "⚠️ EMI Missed!%n" +
                            "👤 %s%n" +
                            "💰 EMI Amount: ₹%s%n" +
                            "📅 Due Date: %s%n" +
                            "❗ Penalty: ₹%s%n" +
                            "Please pay immediately to avoid further charges.",
                    event.getRecipientName(),
                    event.getStringData("emiAmount"),
                    event.getStringData("dueDate"),
                    event.getStringData("penalty"));
            default -> String.format(
                    "🏦 " + BANK_NAME + " Alert*%n" +
                            "👤 %s%n" +
                            "📋 %s%n" +
                            "Contact support for details.",
                    event.getRecipientName(),
                    event.getEventType().toString().replace("_", " ").toLowerCase());
        };
    }
}