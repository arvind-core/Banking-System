package com.BankingSystem.dto.response.transaction;

import com.BankingSystem.entity.transactions.TransactionStatus;
import com.BankingSystem.entity.transactions.TransactionType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {

    private Long id;
    private String transactionReference;
    private TransactionType transactionType;
    private TransactionStatus status;
    private BigDecimal amount;
    private BigDecimal balanceAfterTransaction;
    private String targetAccountNumber;
    private String description;
    private LocalDateTime createdAt;
}