package com.BankingSystem.dto.response.creditcard;

import com.BankingSystem.entity.card.SpendCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CardTransactionResponse {

    private Long id;
    private String transactionReference;
    private BigDecimal amount;
    private String merchantName;
    private SpendCategory category;
    private BigDecimal rewardPointsEarned;
    private BigDecimal availableLimitAfter;
    private String description;
    private LocalDateTime createdAt;
}
