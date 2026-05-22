package com.BankingSystem.entity.card;

public enum DebitCardTransactionStatus {
    SUCCESS,
    FAILED_INSUFFICIENT_BALANCE,
    FAILED_DAILY_LIMIT_EXCEEDED,
    FAILED_CARD_BLOCKED,
    FAILED_INVALID_PIN
}
