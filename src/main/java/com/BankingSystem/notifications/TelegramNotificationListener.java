package com.BankingSystem.notifications;

import com.BankingSystem.BankConfig;
import com.BankingSystem.repo.NotificationPreferenceRepository;
import com.BankingSystem.repo.UserRepository;
import com.BankingSystem.util.NotificationEvent;
import static com.BankingSystem.BankConfig.BANK_NAME;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramNotificationListener {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Async
    @EventListener
    public void handleNotification(NotificationEvent event) {
        try {
            notificationPreferenceRepository
                    .findByUser(userRepository
                            .findByPhoneNumberAndIsActiveTrue(event.getRecipientPhone())
                            .orElse(null))
                            .ifPresent(pref -> {
                                if(pref.isTelegramEnabled() && pref.getTelegramChatId() != null && !pref.getTelegramChatId().isBlank()){

                                    sendTelegramMessage(pref.getTelegramChatId(), buildTelegramMessage(event));
                                }
                            });

        } catch (Exception e) {
            log.error("Failed to send Telegram notification for event: {} | Error: {}",
                    event.getEventType(), e.getMessage());
        }
    }

    private void sendTelegramMessage(String chatId, String message) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", message);
            body.put("parse_mode", "markdown");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);

            log.info("Telegram message sent to chat : {}", chatId);
        }
        catch (Exception e) {
            log.error("Telegram API call failed for chat : {} | Error : {}", chatId, e.getMessage());
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
            case INTEREST_CREDITED -> String.format(
                    "Dear %s, ₹%s has been credited as monthly interest to account %s " +
                            "for %s at %.2f%% annual rate. New balance: ₹%s.",
                    event.getRecipientName(),
                    event.getStringData("interestAmount"),
                    event.getStringData("accountNumber"),
                    event.getStringData("period"),
                    BankConfig.SAVINGS_ACCOUNT_INTEREST_RATE,
                    event.getStringData("newBalance"));
            case BRANCH_TRANSFER_COMPLETED -> String.format(
                    "Dear %s, your account %s has been successfully transferred to %s. " +
                            "Your new IFSC code is %s.",
                    event.getRecipientName(),
                    event.getStringData("accountNumber"),
                    event.getStringData("newBranch"),
                    event.getStringData("newIfscCode"));
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