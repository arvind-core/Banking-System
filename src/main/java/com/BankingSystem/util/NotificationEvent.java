package com.BankingSystem.util;

import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.transactions.Transaction;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Getter
public class NotificationEvent extends ApplicationEvent {

    private final NotificationEventType eventType;
    private final String recipientName;
    private final String recipientEmail;
    private final String recipientPhone;
    private final Map<String, Object> data;

    private NotificationEvent(Object source,
                              NotificationEventType eventType,
                              String recipientName,
                              String recipientEmail,
                              String recipientPhone,
                              Map<String, Object> data) {
        super(source);
        this.eventType = eventType;
        this.recipientName = recipientName;
        this.recipientEmail = recipientEmail;
        this.recipientPhone = recipientPhone;
        this.data = data;
    }

    // Factory method for transaction events
    public static NotificationEvent forTransaction(Object source,
                                                    Transaction transaction,
                                                    Account account) {
        Map<String, Object> data = new HashMap<>();
        data.put("transactionReference", transaction.getTransactionReference());
        data.put("transactionType", transaction.getTransactionType());
        data.put("amount", transaction.getAmount());
        data.put("balance", transaction.getBalanceAfterTransaction());
        data.put("accountNumber", account.getAccountNumber());
        data.put("targetAccountNumber", transaction.getTargetAccountNumber());
        data.put("description", transaction.getDescription());

        NotificationEventType eventType = switch (transaction.getTransactionType()) {
            case DEPOSIT -> NotificationEventType.DEPOSIT;
            case WITHDRAWAL -> NotificationEventType.WITHDRAWAL;
            case TRANSFER_DEBIT -> NotificationEventType.TRANSFER_SENT;
            case TRANSFER_CREDIT -> NotificationEventType.TRANSFER_RECEIVED;
        };

        return new NotificationEvent(
                source,
                eventType,
                account.getUser().getFirstName() + " " + account.getUser().getLastName(),
                account.getUser().getEmail(),
                account.getUser().getPhoneNumber(),
                data
        );
    }

    // Factory method for account events
    public static NotificationEvent forAccount(Object source,
                                                NotificationEventType eventType,
                                                Account account) {
        Map<String, Object> data = new HashMap<>();
        data.put("accountNumber", account.getAccountNumber());
        data.put("accountType", account.getAccountType());
        data.put("branchName", account.getBranchName());

        return new NotificationEvent(
                source,
                eventType,
                account.getUser().getFirstName() + " " + account.getUser().getLastName(),
                account.getUser().getEmail(),
                account.getUser().getPhoneNumber(),
                data
        );
    }

    // Factory method for generic events with custom data
    public static NotificationEvent forUser(Object source,
                                             NotificationEventType eventType,
                                             String recipientName,
                                             String recipientEmail,
                                             String recipientPhone,
                                             Map<String, Object> data) {
        return new NotificationEvent(
                source,
                eventType,
                recipientName,
                recipientEmail,
                recipientPhone,
                data
        );
    }

    // Helper to safely get data values
    public Object getData(String key) {
        return data.get(key);
    }

    public BigDecimal getAmountData(String key) {
        Object value = data.get(key);
        return value != null ? (BigDecimal) value : BigDecimal.ZERO;
    }

    public String getStringData(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : "";
    }
}