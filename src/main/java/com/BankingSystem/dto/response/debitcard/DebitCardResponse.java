package com.BankingSystem.dto.response.debitcard;

import com.BankingSystem.entity.card.DebitCardStatus;
import com.BankingSystem.entity.card.DebitCardType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DebitCardResponse {
    private Long id;
    private String maskedCardNumber;
    private String cardHolderName;
    private DebitCardType cardType;
    private DebitCardStatus status;
    private BigDecimal dailyAtmLimit;
    private BigDecimal dailyPosLimit;
    private BigDecimal dailyOnlineLimit;
    private BigDecimal dailyAtmSpent;
    private BigDecimal dailyPosSpent;
    private BigDecimal dailyOnlineSpent;
    private boolean internationalTransactionsEnabled;
    private boolean onlineTransactionsEnabled;
    private boolean contactlessEnabled;
    private String linkedAccountNumber;
    private BigDecimal accountBalance;
    private LocalDate expiryDate;
    private LocalDateTime createdAt;
}
