package com.BankingSystem.dto.response.creditcard;

import com.BankingSystem.entity.card.CardStatus;
import com.BankingSystem.entity.card.CardType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CreditCardResponse {
    private Long id;
    private String maskedCardNumber;
    private String cardHolderName;
    private CardType cardType;
    private CardStatus cardStatus;
    private BigDecimal creditLimit;
    private BigDecimal availableLimit;
    private BigDecimal outstandingAmount;
    private BigDecimal minimumDue;
    private BigDecimal totalRewardPoints;
    private LocalDate expiryDate;
    private LocalDate nextBillingDate;
    private LocalDate paymentDueDate;
    private boolean internationalTransactionsEnabled;
    private boolean onlineTransactionsEnabled;
    private String linkedAccountNumber;
    private LocalDateTime createdAt;
}
