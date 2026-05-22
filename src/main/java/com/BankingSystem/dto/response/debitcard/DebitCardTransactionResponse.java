package com.BankingSystem.dto.response.debitcard;

import com.BankingSystem.entity.card.DebitCardTransactionStatus;
import com.BankingSystem.entity.card.DebitTransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DebitCardTransactionResponse {

    private Long id;
    private String transactionReference;
    private BigDecimal amount;
    private BigDecimal balanceAfterTransaction;
    private String merchantName;
    private DebitTransactionType transactionType;
    private DebitCardTransactionStatus status;
    private String description;
    private LocalDateTime createdAt;
}
